package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.DetallePedidoClienteDTO;
import com.floristeria.floristeria.dto.DetallePedidoRequestDTO;
import com.floristeria.floristeria.dto.PedidoAdminResponseDTO;
import com.floristeria.floristeria.dto.PedidoClienteRequestDTO;
import com.floristeria.floristeria.dto.PedidoClienteResponseDTO;
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

    @Value("${wompi.public-key}")
    private String wompiPublicKey;

    @Value("${wompi.integrity-secret}")
    private String wompiIntegritySecret;

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

        // Calcular total y validar stock desde Inventario
        BigDecimal total = BigDecimal.ZERO;
        List<DetallePedido> detallesPedidos = new ArrayList<>();

        Pedido pedido = Pedido.builder()
                .sede(sede)
                .cliente(cliente)
                .direccion(direccion)
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

        // Actualizar total y detalles del pedido
        savedPedido.setTotal(total);
        savedPedido.setDetalles(detallesPedidos);
        pedidoRepository.save(savedPedido);

        deducirInventario(savedPedido);

        String referencia = savedPedido.getId() + "-" + System.currentTimeMillis();
        savedPedido.setReferenciaPago(referencia);
        pedidoRepository.save(savedPedido);

        long montoCentavos = savedPedido.getTotal().longValue() * 100;
        String cadena = referencia + montoCentavos + "COP" + wompiIntegritySecret;
        String firmaIntegridad = generarSha256Hex(cadena);

        return PedidoClienteResponseDTO.builder()
                .pedidoId(savedPedido.getId())
                .total(total)
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

        return PedidoAdminResponseDTO.builder()
                .id(pedido.getId())
                .clienteNombre(cliente.getNombre())
                .clienteEmail(cliente.getEmail())
                .clienteTelefono(cliente.getTelefono())
                .direccionAlias(direccion.getAlias())
                .direccionCompleta(direccion.getDireccion())
                .direccionCiudad(direccion.getCiudad())
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
        pedidoRepository.save(pedido);

        deducirInventario(pedido);
    }
}
