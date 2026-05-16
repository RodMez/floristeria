package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.DetallePedidoRequestDTO;
import com.floristeria.floristeria.dto.PedidoAdminResponseDTO;
import com.floristeria.floristeria.dto.PedidoRequestDTO;
import com.floristeria.floristeria.entity.DetallePedido;
import com.floristeria.floristeria.entity.Pedido;
import com.floristeria.floristeria.entity.Producto;
import com.floristeria.floristeria.entity.Sede;
import com.floristeria.floristeria.repository.DetallePedidoRepository;
import com.floristeria.floristeria.repository.PedidoRepository;
import com.floristeria.floristeria.repository.ProductoRepository;
import com.floristeria.floristeria.repository.SedeRepository;
import com.floristeria.floristeria.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final SedeRepository sedeRepository;
    private final ProductoRepository productoRepository;

    @Transactional
    @Override
    public Integer crearPedido(PedidoRequestDTO request) {
        Sede sede = sedeRepository.findById(request.getSedeId())
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada"));

        BigDecimal total = request.getDetalles().stream()
                .map(detalle -> detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Pedido pedido = Pedido.builder()
                .sede(sede)
                .clienteNombre(request.getClienteNombre())
                .clienteTelefono(request.getClienteTelefono())
                .notasEntrega(request.getNotasEntrega())
                .total(total)
                .estado("PENDIENTE")
                .creadoEn(LocalDateTime.now())
                .build();

        Pedido savedPedido = pedidoRepository.save(pedido);

        for (DetallePedidoRequestDTO detalleRequest : request.getDetalles()) {
            Producto producto = productoRepository.findById(detalleRequest.getProductoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

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
        return pedidoRepository.findBySede_Id(sedeId).stream()
                .map(pedido -> {
                    PedidoAdminResponseDTO dto = new PedidoAdminResponseDTO();
                    dto.setId(pedido.getId());
                    dto.setClienteNombre(pedido.getClienteNombre());
                    dto.setClienteTelefono(pedido.getClienteTelefono());
                    dto.setTotal(pedido.getTotal());
                    dto.setEstado(pedido.getEstado());
                    dto.setCreadoEn(pedido.getCreadoEn());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public PedidoAdminResponseDTO actualizarEstadoPedido(Integer pedidoId, String nuevoEstado, Integer usuarioSedeId, String rol) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado"));

        if (!"SUPERADMIN".equals(rol) && !pedido.getSede().getId().equals(usuarioSedeId)) {
            throw new AccessDeniedException("No tiene permisos sobre este pedido");
        }

        pedido.setEstado(nuevoEstado);
        pedidoRepository.save(pedido);

        PedidoAdminResponseDTO dto = new PedidoAdminResponseDTO();
        dto.setId(pedido.getId());
        dto.setClienteNombre(pedido.getClienteNombre());
        dto.setClienteTelefono(pedido.getClienteTelefono());
        dto.setTotal(pedido.getTotal());
        dto.setEstado(pedido.getEstado());
        dto.setCreadoEn(pedido.getCreadoEn());
        return dto;
    }
}
