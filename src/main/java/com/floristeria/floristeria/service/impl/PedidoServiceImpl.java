package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.DetallePedidoRequestDTO;
import com.floristeria.floristeria.dto.PedidoAdminResponseDTO;
import com.floristeria.floristeria.dto.PedidoRequestDTO;
import com.floristeria.floristeria.entity.*;
import com.floristeria.floristeria.repository.*;
import com.floristeria.floristeria.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
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
}
