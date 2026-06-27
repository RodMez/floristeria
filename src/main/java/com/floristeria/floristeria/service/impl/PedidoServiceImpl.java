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
import com.floristeria.floristeria.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final ZonaDomicilioRepository zonaDomicilioRepository;

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

        BigDecimal total = request.getDetalles().stream()
                .map(detalle -> detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Pedido pedido = Pedido.builder()
                .sede(sede)
                .cliente(cliente)
                .direccion(direccion)
                .notasEntrega(request.getNotasEntrega())
                .total(total)
                .build();

        Pedido savedPedido = pedidoRepository.save(pedido);

        for (DetallePedidoRequestDTO detalleRequest : request.getDetalles()) {
            Producto producto = productoRepository.findById(detalleRequest.getProductoId())
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));

            DetallePedido detallePedido = DetallePedido.builder()
                    .pedido(savedPedido)
                    .producto(producto)
                    .cantidad(detalleRequest.getCantidad())
                    .precioUnitario(detalleRequest.getPrecioUnitario())
                    .notaPersonalizacion(detalleRequest.getNotaPersonalizacion())
                    .build();

            detallePedidoRepository.save(detallePedido);
        }

        return savedPedido.getId();
    }

    @Transactional
    @Override
    public PedidoClienteResponseDTO crearPedidoCliente(PedidoClienteRequestDTO request, Integer clienteId) {
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

        // Validar zona de domicilio
        ZonaDomicilio zonaDomicilio = zonaDomicilioRepository.findById(request.getZonaDomicilioId())
                .orElseThrow(() -> new EntityNotFoundException("Zona de domicilio no encontrada"));

        if (!zonaDomicilio.getSede().getId().equals(request.getSedeId())) {
            throw new IllegalArgumentException("La zona de domicilio no pertenece a esta sede");
        }

        // Calcular total y validar stock desde Inventario
        BigDecimal total = BigDecimal.ZERO;
        List<DetallePedido> detallesPedidos = new ArrayList<>();

        Pedido pedido = Pedido.builder()
                .sede(sede)
                .cliente(cliente)
                .direccion(direccion)
                .zonaDomicilio(zonaDomicilio)
                .costoEnvio(zonaDomicilio.getPrecio())
                .notasEntrega(request.getNotasEntrega())
                .total(BigDecimal.ZERO) // Temporal, se actualiza después
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

            // Usar precio del inventario (precio específico de la sede)
            BigDecimal precioUnitario = inventario.getPrecio();
            BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(detalleRequest.getCantidad()));
            total = total.add(subtotal);

            DetallePedido detallePedido = DetallePedido.builder()
                    .pedido(savedPedido)
                    .producto(producto)
                    .cantidad(detalleRequest.getCantidad())
                    .precioUnitario(precioUnitario)
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

        String referencia = savedPedido.getId() + "-" + System.currentTimeMillis();
        savedPedido.setReferenciaPago(referencia);
        pedidoRepository.save(savedPedido);

        long montoCentavos = savedPedido.getTotal().longValue() * 100;
        String cadena = referencia + montoCentavos + "COP" + wompiIntegritySecret;
        String firmaIntegridad = generarSha256Hex(cadena);

        return PedidoClienteResponseDTO.builder()
                .pedidoId(savedPedido.getId())
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
    public PedidoAdminResponseDTO actualizarEstadoPedido(Integer pedidoId, EstadoPedido nuevoEstado, Integer usuarioSedeId, String rol) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
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
                .id(pedido.getId())
                .clienteNombre(cliente != null ? cliente.getNombre() : "Cliente eliminado")
                .clienteEmail(cliente != null ? cliente.getEmail() : "N/A")
                .clienteTelefono(cliente != null ? cliente.getTelefono() : "N/A")
                .sedeNombre(sede != null ? sede.getNombre() : "Sede eliminada")
                .metodoPago(pedido.getMetodoPago())
                .referenciaPago(pedido.getReferenciaPago())
                .direccionEntrega(direccionEntrega)
                .detalles(detalles)
                .total(pedido.getTotal())
                .estado(pedido.getEstado().name())
                .transaccionId(pedido.getTransaccionId())
                .creadoEn(pedido.getCreadoEn())
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

        if (!firmaCalculada.equals(checksum)) {
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

        if (pedido.getEstado() == EstadoPedido.PAGADO) {
            return;
        }

        if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) {
            return;
        }

        pedido.setEstado(EstadoPedido.PAGADO);
        pedido.setTransaccionId(transactionId);
        pedido.setMetodoPago(paymentMethodType);
        pedidoRepository.save(pedido);

        deducirInventario(pedido);
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
                .id(pedido.getId())
                .total(pedido.getTotal())
                .estado(pedido.getEstado().name())
                .creadoEn(pedido.getCreadoEn())
                .referenciaPago(pedido.getReferenciaPago())
                .sedeNombre(sede != null ? sede.getNombre() : "Sede eliminada")
                .metodoPago(pedido.getMetodoPago() != null ? pedido.getMetodoPago() : "No especificado")
                .direccionEntrega(direccionEntrega)
                .detalles(detalles)
                .build();
    }
}
