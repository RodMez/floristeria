package com.floristeria.floristeria.service;

import com.floristeria.floristeria.dto.DetallePedidoRequestDTO;
import com.floristeria.floristeria.dto.PedidoRequestDTO;
import com.floristeria.floristeria.entity.DetallePedido;
import com.floristeria.floristeria.entity.Pedido;
import com.floristeria.floristeria.entity.Producto;
import com.floristeria.floristeria.entity.Sede;
import com.floristeria.floristeria.repository.DetallePedidoRepository;
import com.floristeria.floristeria.repository.PedidoRepository;
import com.floristeria.floristeria.repository.ProductoRepository;
import com.floristeria.floristeria.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final SedeRepository sedeRepository;
    private final ProductoRepository productoRepository;

    @Transactional
    public Integer crearPedido(PedidoRequestDTO request) {
        // 1. Buscar la sede
        Sede sede = sedeRepository.findById(request.getSedeId())
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada"));

        // 2. Calcular el total usando BigDecimal 
        BigDecimal total = request.getDetalles().stream()
                .map(detalle -> detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Crear el pedido
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

        // 4. Guardar los detalles
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
}