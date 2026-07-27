package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.DetallePedidoClienteDTO;
import com.floristeria.floristeria.dto.DetallePedidoRequestDTO;
import com.floristeria.floristeria.dto.PedidoAdminResponseDTO;
import com.floristeria.floristeria.dto.PedidoClienteRequestDTO;
import com.floristeria.floristeria.dto.PedidoClienteResponseDTO;
import com.floristeria.floristeria.dto.PedidoHistorialDTO;
import com.floristeria.floristeria.dto.PedidoRequestDTO;
import com.floristeria.floristeria.entity.*;
import com.floristeria.floristeria.repository.*;
import com.floristeria.floristeria.service.ConfiguracionTiendaService;
import com.floristeria.floristeria.service.EmailService;
import com.floristeria.floristeria.service.PedidoService;
import com.floristeria.floristeria.exception.ZonaExcluidaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;

import org.hibernate.Hibernate;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final SedeRepository sedeRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final DireccionRepository direccionRepository;
    private final InventarioRepository inventarioRepository;
    private final EmailService emailService;
    private final ConfiguracionTiendaService configuracionService;

    @Value("${wompi.public-key}")
    private String wompiPublicKey;

    @Value("${wompi.integrity-secret}")
    private String wompiIntegritySecret;

    @Value("${wompi.events-secret}")
    private String wompiEventsSecret;

    @Transactional
    @Override
    public Integer crearPedido(PedidoRequestDTO request) {
        Sede sede = sedeRepository.findById(request.getSedeId())
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada"));

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

        Direccion direccion = direccionRepository.findById(request.getDireccionId())
                .orElseThrow(() -> new EntityNotFoundException("Dirección no encontrada"));

        BigDecimal total = BigDecimal.ZERO;
        List<DetallePedido> detallesPedidos = new ArrayList<>();

        Pedido pedido = Pedido.builder()
                .sede(sede)
                .sedeNombre(sede.getNombre())
                .cliente(cliente)
                .direccion(direccion)
                .notasEntrega(request.getNotasEntrega())
                .total(BigDecimal.ZERO)
                .build();

        Pedido savedPedido = pedidoRepository.save(pedido);

        for (DetallePedidoRequestDTO detalleRequest : request.getDetalles()) {
            Inventario inventario = inventarioRepository.findByProducto_IdAndSede_Id(
                    detalleRequest.getProductoId(), request.getSedeId());

            if (inventario == null) {
                throw new EntityNotFoundException(
                        "Producto no disponible en la sede seleccionada (producto ID: " + detalleRequest.getProductoId() + ")");
            }

            if (!inventario.getDisponible() || inventario.getStock() < detalleRequest.getCantidad()) {
                throw new IllegalStateException(
                        "Stock insuficiente para el producto ID: " + detalleRequest.getProductoId());
            }

            Producto producto = inventario.getProducto();

            BigDecimal precioBase = inventario.getPrecio();
            Integer descuento = inventario.getDescuentoPorcentaje();
            BigDecimal precioFinal;

            if (descuento != null && descuento > 0) {
                BigDecimal descuentoAmount = precioBase.multiply(new BigDecimal(descuento))
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                precioFinal = precioBase.subtract(descuentoAmount);
            } else {
                precioFinal = precioBase;
            }

            BigDecimal subtotal = precioFinal.multiply(BigDecimal.valueOf(detalleRequest.getCantidad()));
            total = total.add(subtotal);

            DetallePedido detallePedido = DetallePedido.builder()
                    .pedido(savedPedido)
                    .producto(producto)
                    .cantidad(detalleRequest.getCantidad())
                    .precioUnitario(precioFinal)
                    .notaPersonalizacion(detalleRequest.getNotaPersonalizacion())
                    .build();

            detallePedidoRepository.save(detallePedido);
            detallesPedidos.add(detallePedido);
        }

        savedPedido.setTotal(total);
        savedPedido.setDetalles(detallesPedidos);
        pedidoRepository.save(savedPedido);

        return savedPedido.getId();
    }

    @Transactional
    @Override
    public PedidoClienteResponseDTO crearPedidoCliente(PedidoClienteRequestDTO request, Integer clienteId) {
        if (!Boolean.TRUE.equals(request.getAceptaTerminos())) {
            throw new IllegalArgumentException("Debes aceptar los términos y condiciones para confirmar el pedido");
        }

        // Validar que la sede existe
        Sede sede = sedeRepository.findById(request.getSedeId())
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada"));

        // Validar que el cliente existe
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

        // Validar que la dirección pertenece al cliente (Prevención IDOR)
        Direccion direccion = direccionRepository.findById(request.getDireccionId())
                .orElseThrow(() -> new EntityNotFoundException("Dirección no encontrada"));

        if (!direccion.getCliente().getId().equals(clienteId)) {
            throw new AccessDeniedException("La dirección no pertenece al cliente autenticado");
        }

        // Validar que la dirección esté en la misma ciudad que la sede
        if (!direccion.getCiudad().equalsIgnoreCase(sede.getCiudad())) {
            throw new IllegalArgumentException(
                    "La dirección de entrega debe estar en la misma ciudad que la sede de la tienda (" + sede.getCiudad() + ").");
        }

        // Obtener zona de domicilio desde la dirección del cliente
        ZonaDomicilio zonaDomicilio = direccion.getZonaDomicilio();
        if (zonaDomicilio == null) {
            throw new IllegalArgumentException("La dirección no tiene una zona de domicilio asignada. Asigna una zona a tu dirección antes de crear un pedido.");
        }

        // Validar que la zona pertenece a la sede
        if (!zonaDomicilio.getSede().getId().equals(request.getSedeId())) {
            throw new IllegalArgumentException("La zona de domicilio de la dirección no pertenece a esta sede");
        }

        // Validar que la zona no esté excluida del domicilio
        if (Boolean.TRUE.equals(zonaDomicilio.getExcluido())) {
            throw new ZonaExcluidaException("Esta zona de domicilio no está disponible para domicilio. Puedes contactarnos por WhatsApp para realizar tu pedido.");
        }

        // Calcular total y validar stock desde Inventario
        BigDecimal total = BigDecimal.ZERO;
        List<DetallePedido> detallesPedidos = new ArrayList<>();

        Pedido pedido = Pedido.builder()
                .sede(sede)
                .sedeNombre(sede.getNombre())
                .cliente(cliente)
                .direccion(direccion)
                .costoEnvio(zonaDomicilio.getPrecio())
                .notasEntrega(request.getNotasEntrega())
                .total(BigDecimal.ZERO) // Temporal, se actualiza después
                .aceptaTerminos(true)
                .fechaAceptacionTyc(LocalDateTime.now())
                .versionTyc("v1")
                .build();

        Pedido savedPedido = pedidoRepository.save(pedido);

        for (DetallePedidoClienteDTO detalleRequest : request.getDetalles()) {
            // Buscar inventario en la sede específica
            Inventario inventario = inventarioRepository.findByProducto_IdAndSede_Id(
                    detalleRequest.getProductoId(), request.getSedeId());

            if (inventario == null) {
                throw new EntityNotFoundException(
                        "Producto no disponible en la sede seleccionada");
            }

            // Validar disponibilidad y stock
            if (!inventario.getDisponible() || inventario.getStock() < detalleRequest.getCantidad()) {
                throw new IllegalStateException(
                        "Stock insuficiente para el producto ID: " + detalleRequest.getProductoId());
            }

            Producto producto = inventario.getProducto();

            // Calcular precio con descuento si aplica
            BigDecimal precioBase = inventario.getPrecio();
            Integer descuento = inventario.getDescuentoPorcentaje();
            BigDecimal precioFinal;

            if (descuento != null && descuento > 0) {
                BigDecimal descuentoAmount = precioBase.multiply(new BigDecimal(descuento))
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                precioFinal = precioBase.subtract(descuentoAmount);
            } else {
                precioFinal = precioBase;
            }

            BigDecimal subtotal = precioFinal.multiply(BigDecimal.valueOf(detalleRequest.getCantidad()));
            total = total.add(subtotal);

            DetallePedido detallePedido = DetallePedido.builder()
                    .pedido(savedPedido)
                    .producto(producto)
                    .cantidad(detalleRequest.getCantidad())
                    .precioUnitario(precioFinal)
                    .notaPersonalizacion(detalleRequest.getNotaPersonalizacion())
                    .build();

            detallePedidoRepository.save(detallePedido);
            detallesPedidos.add(detallePedido);
        }

        // Actualizar total con costo de envío y detalles del pedido
        BigDecimal totalConEnvio = total.add(zonaDomicilio.getPrecio());
        savedPedido.setTotal(totalConEnvio);
        savedPedido.setDetalles(detallesPedidos);
        pedidoRepository.save(savedPedido);

        String referencia = savedPedido.getCodigo() + "-" + System.currentTimeMillis();
        savedPedido.setReferenciaPago(referencia);
        pedidoRepository.save(savedPedido);

        long montoCentavos = savedPedido.getTotal().longValue() * 100;
        String cadena = referencia + montoCentavos + "COP" + wompiIntegritySecret;
        String firmaIntegridad = generarSha256Hex(cadena);

        return PedidoClienteResponseDTO.builder()
                .pedidoId(savedPedido.getCodigo())
                .total(totalConEnvio)
                .estado(savedPedido.getEstado().name())
                .referenciaWompi(referencia)
                .montoEnCentavos(montoCentavos)
                .firmaIntegridad(firmaIntegridad)
                .publicKeyWompi(wompiPublicKey)
                .build();
    }

    private String generarSha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 no disponible en el JVM", e);
        }
    }

    @Override
    public List<PedidoAdminResponseDTO> obtenerPedidosPorSede(Integer sedeId) {
        List<Pedido> pedidos = (sedeId == null)
                ? pedidoRepository.findAll()
                : pedidoRepository.findBySede_Id(sedeId);

        return pedidos.stream()
                .map(this::mapToAdminResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public PedidoAdminResponseDTO actualizarEstadoPedido(String pedidoCodigo, EstadoPedido nuevoEstado, Integer usuarioSedeId, String rol) {
        Pedido pedido = pedidoRepository.findByCodigo(pedidoCodigo)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado"));

        if (!"SUPERADMIN".equals(rol) && !pedido.getSede().getId().equals(usuarioSedeId)) {
            throw new AccessDeniedException("No tiene permisos sobre este pedido");
        }

        if (nuevoEstado == EstadoPedido.PAGADO) {
            throw new IllegalStateException("El pago solo puede ser procesado automáticamente por la pasarela.");
        }

        if (pedido.getEstado() == EstadoPedido.PENDIENTE_PAGO && nuevoEstado != EstadoPedido.CANCELADO) {
            throw new IllegalStateException("Desde PENDIENTE_PAGO solo se permite cancelar el pedido.");
        }

        pedido.setEstado(nuevoEstado);
        pedidoRepository.save(pedido);

        return mapToAdminResponseDTO(pedido);
    }

    private PedidoAdminResponseDTO mapToAdminResponseDTO(Pedido pedido) {
        Cliente cliente = pedido.getCliente();
        Direccion direccion = pedido.getDireccion();
        Sede sede = pedido.getSede();

        PedidoAdminResponseDTO.DireccionEntregaDTO direccionEntrega = direccion != null
                ? PedidoAdminResponseDTO.DireccionEntregaDTO.builder()
                        .alias(direccion.getAlias())
                        .direccion(direccion.getDireccion())
                        .ciudad(direccion.getCiudad())
                        .detalles(direccion.getDetalles())
                        .build()
                : PedidoAdminResponseDTO.DireccionEntregaDTO.builder()
                        .alias("Dirección eliminada")
                        .direccion("N/A")
                        .ciudad("N/A")
                        .detalles("")
                        .build();

        List<PedidoAdminResponseDTO.DetallePedidoAdminDTO> detalles =
                pedido.getDetalles() != null
                        ? pedido.getDetalles().stream()
                                .map(d -> PedidoAdminResponseDTO.DetallePedidoAdminDTO.builder()
                                        .productoNombre(d.getProducto() != null
                                                ? d.getProducto().getNombre() : "Producto eliminado")
                                        .productoSku(d.getProducto() != null
                                                ? d.getProducto().getSku() : "N/A")
                                        .cantidad(d.getCantidad())
                                        .precioUnitario(d.getPrecioUnitario())
                                        .notaPersonalizacion(d.getNotaPersonalizacion())
                                        .build())
                                .collect(Collectors.toList())
                        : new ArrayList<>();

        return PedidoAdminResponseDTO.builder()
                .id(pedido.getCodigo() != null ? pedido.getCodigo() : String.valueOf(pedido.getId()))
                .clienteNombre(cliente != null ? cliente.getNombre() : "Cliente eliminado")
                .clienteEmail(cliente != null ? cliente.getEmail() : "N/A")
                .clienteTelefono(cliente != null ? cliente.getTelefono() : "N/A")
                .sedeId(sede != null ? sede.getId() : null)
                .sedeNombre(sede != null ? sede.getNombre() : (pedido.getSedeNombre() != null ? pedido.getSedeNombre() : "Sede eliminada"))
                .metodoPago(pedido.getMetodoPago())
                .referenciaPago(pedido.getReferenciaPago())
                .direccionEntrega(direccionEntrega)
                .detalles(detalles)
                .total(pedido.getTotal())
                .estado(pedido.getEstado().name())
                .transaccionId(pedido.getTransaccionId())
                .creadoEn(pedido.getCreadoEn())
                .costoEnvio(pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : BigDecimal.ZERO)
                .zonaDomicilioNombre(
                    direccion != null && direccion.getZonaDomicilio() != null
                        ? direccion.getZonaDomicilio().getLocalidad()
                          + (direccion.getZonaDomicilio().getBarrio() != null
                              ? " - " + direccion.getZonaDomicilio().getBarrio()
                              : "")
                        : "Zona no especificada"
                )
                .notasEntrega(pedido.getNotasEntrega())
                .build();
    }

    private void deducirInventario(Pedido pedido) {
        for (DetallePedido detalle : pedido.getDetalles()) {
            Inventario inventario = inventarioRepository.findByProducto_IdAndSede_Id(
                    detalle.getProducto().getId(), pedido.getSede().getId());

            if (inventario == null || !inventario.getDisponible()
                    || inventario.getStock() < detalle.getCantidad()) {
                throw new IllegalStateException(
                        "Stock insuficiente para el producto ID: " + detalle.getProducto().getId());
            }

            inventario.setStock(inventario.getStock() - detalle.getCantidad());
            if (inventario.getStock() == 0) {
                inventario.setDisponible(false);
            }
            inventarioRepository.save(inventario);
        }
    }

    @Transactional
    @Override
    public void procesarPagoExitoso(Integer pedidoId, String transaccionId, String metodoPago) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado"));

        if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) {
            throw new IllegalStateException(
                    "No se puede procesar el pago de un pedido en estado: " + pedido.getEstado());
        }

        pedido.setEstado(EstadoPedido.PAGADO);
        pedido.setTransaccionId(transaccionId);
        pedido.setMetodoPago(metodoPago);
        pedidoRepository.save(pedido);

        deducirInventario(pedido);
    }

    @Transactional
    @Override
    public void procesarWebhookWompi(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        if (data == null) {
            throw new IllegalArgumentException("Payload no contiene nodo 'data'");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> transaction = (Map<String, Object>) data.get("transaction");
        if (transaction == null) {
            throw new IllegalArgumentException("Payload no contiene 'data.transaction'");
        }

        String transactionId = (String) transaction.get("id");
        String status = (String) transaction.get("status");
        Object amountObj = transaction.get("amount_in_cents");
        String reference = (String) transaction.get("reference");
        String paymentMethodType = (String) transaction.get("payment_method_type");

        @SuppressWarnings("unchecked")
        Map<String, Object> signature = (Map<String, Object>) payload.get("signature");
        if (signature == null) {
            throw new IllegalArgumentException("Payload no contiene 'signature'");
        }
        String checksum = (String) signature.get("checksum");

        Object timestamp = payload.get("timestamp");
        Long timestampSecs = (timestamp instanceof Number) ? ((Number) timestamp).longValue() : null;

        if (transactionId == null || status == null || amountObj == null || checksum == null || timestampSecs == null) {
            throw new IllegalArgumentException("Payload incompleto para validación de firma");
        }

        String amountInCents = amountObj.toString();
        String cadenaFirma = transactionId + status + amountInCents + timestampSecs + wompiEventsSecret;
        String firmaCalculada = generarSha256Hex(cadenaFirma);

        if (!MessageDigest.isEqual(firmaCalculada.getBytes(StandardCharsets.UTF_8), checksum.getBytes(StandardCharsets.UTF_8))) {
            throw new SecurityException("Firma del webhook inválida");
        }

        if (!"APPROVED".equalsIgnoreCase(status)) {
            return;
        }

        Pedido pedido = pedidoRepository.findByReferenciaPago(reference)
                .orElse(null);

        if (pedido == null) {
            return;
        }

        long montoEsperadoCentavos = pedido.getTotal().multiply(BigDecimal.valueOf(100)).longValue();
        if (!String.valueOf(montoEsperadoCentavos).equals(amountInCents)) {
            log.error("Webhook Wompi - monto mismatch: esperado={} recibido={} pedido={}",
                    montoEsperadoCentavos, amountInCents, pedido.getCodigo());
            return;
        }

        if (pedido.getEstado() == EstadoPedido.PAGADO) {
            return;
        }

        if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) {
            return;
        }

        marcarPedidoPagado(pedido, transactionId, paymentMethodType);

        try {
            deducirInventario(pedido);
        } catch (IllegalStateException e) {
            log.error("Webhook Wompi - stock insuficiente post-pago para pedido {}: {}",
                    pedido.getCodigo(), e.getMessage());
            notificarAlertaStockInsuficiente(pedido, e.getMessage());
        }

        emailService.notificarNuevaVenta(pedido);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void marcarPedidoPagado(Pedido pedido, String transactionId, String paymentMethodType) {
        Hibernate.initialize(pedido.getDetalles());
        pedido.setEstado(EstadoPedido.PAGADO);
        pedido.setTransaccionId(transactionId);
        pedido.setMetodoPago(paymentMethodType);
        pedidoRepository.save(pedido);
    }

    private void notificarAlertaStockInsuficiente(Pedido pedido, String detalleError) {
        try {
            ConfiguracionTienda config = configuracionService.obtenerConfiguracion();
            if (Boolean.TRUE.equals(config.getEnviarCopiaMaestro())
                    && config.getCorreoMaestro() != null
                    && !config.getCorreoMaestro().isBlank()) {

                String nombreSede = pedido.getSede() != null ? pedido.getSede().getNombre() : "N/A";
                String asunto = "\u26A0\uFE0F STOCK INSUFICIENTE POST-PAGO - Pedido " + pedido.getCodigo();
                String html = construirHtmlAlertaStock(pedido, nombreSede, detalleError);

                emailService.enviarCorreoDirecto(config.getCorreoMaestro(), "Administrador", asunto, html);
            }
        } catch (Exception e) {
            log.error("No se pudo enviar alerta de stock insuficiente para pedido {}: {}",
                    pedido.getCodigo(), e.getMessage());
        }
    }

    private String construirHtmlAlertaStock(Pedido pedido, String nombreSede, String detalleError) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background:#FAFAF9;'>");
        sb.append("<div style='background:#DC2626;color:#fff;padding:20px;text-align:center;'>");
        sb.append("<h1 style='margin:0;font-size:20px;'>\u26A0\uFE0F ALERTA: Stock Insuficiente Post-Pago</h1></div>");
        sb.append("<div style='padding:24px;background:#fff;border:1px solid #E7E5E4;'>");
        sb.append("<p style='color:#3D3D3D;font-size:15px;'>Se confirmó el pago del pedido pero no se pudo deducir el inventario:</p>");
        sb.append("<table style='width:100%;border-collapse:collapse;font-size:14px;'>");
        sb.append("<tr><td style='padding:8px 0;color:#78716C;font-weight:bold;'>Pedido:</td><td style='padding:8px 0;color:#3D3D3D;'>").append(pedido.getCodigo()).append("</td></tr>");
        sb.append("<tr><td style='padding:8px 0;color:#78716C;font-weight:bold;'>Sede:</td><td style='padding:8px 0;color:#3D3D3D;'>").append(nombreSede).append("</td></tr>");
        sb.append("<tr><td style='padding:8px 0;color:#78716C;font-weight:bold;'>Total cobrado:</td><td style='padding:8px 0;color:#3D3D3D;'>$").append(pedido.getTotal()).append(" COP</td></tr>");
        sb.append("<tr><td style='padding:8px 0;color:#78716C;font-weight:bold;'>Cliente:</td><td style='padding:8px 0;color:#3D3D3D;'>").append(pedido.getCliente() != null ? pedido.getCliente().getNombre() : "N/A").append("</td></tr>");
        sb.append("<tr><td style='padding:8px 0;color:#78716C;font-weight:bold;'>Error:</td><td style='padding:8px 0;color:#DC2626;'>").append(detalleError).append("</td></tr>");
        sb.append("</table>");
        sb.append("<p style='color:#78716C;font-size:13px;margin-top:20px;'>El pedido est\u00E1 marcado como PAGADO. Requiere intervenci\u00F3n manual para resolver el inventario.</p>");
        sb.append("</div></div>");
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoHistorialDTO> obtenerPedidosPorCliente(Integer clienteId) {
        List<Pedido> pedidos = pedidoRepository.findByCliente_IdOrderByCreadoEnDesc(clienteId);

        return pedidos.stream()
                .map(this::mapToHistorialDTO)
                .collect(Collectors.toList());
    }

    private PedidoHistorialDTO mapToHistorialDTO(Pedido pedido) {
        Sede sede = pedido.getSede();
        Direccion direccion = pedido.getDireccion();
        Cliente cliente = pedido.getCliente();

        PedidoHistorialDTO.DireccionHistorialDTO direccionEntrega = direccion != null
                ? PedidoHistorialDTO.DireccionHistorialDTO.builder()
                        .alias(direccion.getAlias())
                        .direccion(direccion.getDireccion())
                        .ciudad(direccion.getCiudad())
                        .detalles(direccion.getDetalles())
                        .build()
                : PedidoHistorialDTO.DireccionHistorialDTO.builder()
                        .alias("Dirección eliminada")
                        .direccion("N/A")
                        .ciudad("N/A")
                        .detalles("")
                        .build();

        List<PedidoHistorialDTO.DetalleHistorialDTO> detalles =
                pedido.getDetalles() != null
                        ? pedido.getDetalles().stream()
                                .map(d -> PedidoHistorialDTO.DetalleHistorialDTO.builder()
                                        .productoId(d.getProducto() != null
                                                ? d.getProducto().getId() : null)
                                        .productoNombre(d.getProducto() != null
                                                ? d.getProducto().getNombre() : "Producto eliminado")
                                        .productoSku(d.getProducto() != null
                                                ? d.getProducto().getSku() : "N/A")
                                        .cantidad(d.getCantidad())
                                        .precioUnitario(d.getPrecioUnitario())
                                        .notaPersonalizacion(d.getNotaPersonalizacion())
                                        .build())
                                .collect(Collectors.toList())
                        : new ArrayList<>();

        return PedidoHistorialDTO.builder()
                .id(pedido.getCodigo() != null ? pedido.getCodigo() : String.valueOf(pedido.getId()))
                .total(pedido.getTotal())
                .estado(pedido.getEstado().name())
                .creadoEn(pedido.getCreadoEn())
                .referenciaPago(pedido.getReferenciaPago())
                .sedeNombre(sede != null ? sede.getNombre() : (pedido.getSedeNombre() != null ? pedido.getSedeNombre() : "Sede eliminada"))
                .metodoPago(pedido.getMetodoPago() != null ? pedido.getMetodoPago() : "No especificado")
                .direccionEntrega(direccionEntrega)
                .detalles(detalles)
                .costoEnvio(pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : BigDecimal.ZERO)
                .clienteNombre(cliente != null ? cliente.getNombre() : "Cliente eliminado")
                .clienteEmail(cliente != null ? cliente.getEmail() : "N/A")
                .clienteTelefono(cliente != null ? cliente.getTelefono() : "N/A")
                .zonaDomicilioNombre(
                    direccion != null && direccion.getZonaDomicilio() != null
                        ? direccion.getZonaDomicilio().getLocalidad()
                          + (direccion.getZonaDomicilio().getBarrio() != null
                              ? " - " + direccion.getZonaDomicilio().getBarrio()
                              : "")
                        : "Zona no especificada"
                )
                .build();
    }
}
