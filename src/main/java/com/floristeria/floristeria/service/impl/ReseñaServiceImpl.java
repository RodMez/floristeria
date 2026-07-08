package com.floristeria.floristeria.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floristeria.floristeria.dto.ReseñaEstadoDTO;
import com.floristeria.floristeria.dto.ReseñaRequestDTO;
import com.floristeria.floristeria.dto.ReseñaResponseDTO;
import com.floristeria.floristeria.dto.ReseñasProductoResponseDTO;
import com.floristeria.floristeria.entity.Cliente;
import com.floristeria.floristeria.entity.EstadoPedido;
import com.floristeria.floristeria.entity.Producto;
import com.floristeria.floristeria.entity.Reseña;
import com.floristeria.floristeria.repository.ClienteRepository;
import com.floristeria.floristeria.repository.PedidoRepository;
import com.floristeria.floristeria.repository.ProductoRepository;
import com.floristeria.floristeria.repository.ReseñaRepository;
import com.floristeria.floristeria.service.ReseñaService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReseñaServiceImpl implements ReseñaService {

    private final ReseñaRepository reseñaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;

    @Override
    @Transactional
    public ReseñaResponseDTO crear(Integer clienteId, ReseñaRequestDTO request) {
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

        boolean haComprado = pedidoRepository.existsByClienteIdAndProductoIdAndEstado(
                clienteId, request.getProductoId(), EstadoPedido.ENTREGADO);

        if (!haComprado) {
            throw new IllegalStateException("Debes haber comprado y recibido este producto para poder reseñarlo");
        }

        boolean yaReseñó = reseñaRepository
                .findByProducto_IdAndCliente_IdAndDeletedAtIsNull(request.getProductoId(), clienteId)
                .isPresent();

        if (yaReseñó) {
            throw new IllegalStateException("Ya has reseñado este producto anteriormente");
        }

        Reseña reseña = Reseña.builder()
                .producto(producto)
                .cliente(cliente)
                .calificacion(request.getCalificacion())
                .comentario(request.getComentario())
                .aprobada(false)
                .build();

        reseña = reseñaRepository.save(reseña);

        return mapToResponseDTO(reseña);
    }

    @Override
    @Transactional(readOnly = true)
    public ReseñasProductoResponseDTO obtenerPorProducto(Integer productoId) {
        List<Reseña> reseñas = reseñaRepository
                .findByProducto_IdAndAprobadaTrueAndDeletedAtIsNullOrderByCreadoEnDesc(productoId);

        Double promedio = reseñaRepository.findAverageRatingByProductoId(productoId);
        Integer total = reseñaRepository.findCountByProductoId(productoId);

        List<ReseñaResponseDTO> dtos = reseñas.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        return ReseñasProductoResponseDTO.builder()
                .promedio(promedio != null ? promedio : 0.0)
                .total(total != null ? total : 0)
                .reseñas(dtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReseñaEstadoDTO obtenerEstadoCliente(Integer clienteId, Integer productoId) {
        boolean haComprado = pedidoRepository.existsByClienteIdAndProductoIdAndEstado(
                clienteId, productoId, EstadoPedido.ENTREGADO);

        ReseñaResponseDTO miReseña = reseñaRepository
                .findByProducto_IdAndCliente_IdAndDeletedAtIsNull(productoId, clienteId)
                .map(this::mapToResponseDTO)
                .orElse(null);

        return ReseñaEstadoDTO.builder()
                .puedeCrear(haComprado && miReseña == null)
                .miReseña(miReseña)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReseñaResponseDTO> listarTodas() {
        return reseñaRepository.findAllByDeletedAtIsNullOrderByCreadoEnDesc().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReseñaResponseDTO> listarPendientes() {
        return reseñaRepository.findByAprobadaFalseAndDeletedAtIsNullOrderByCreadoEnDesc().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReseñaResponseDTO aprobar(Integer id) {
        Reseña reseña = reseñaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reseña no encontrada"));

        reseña.setAprobada(true);
        reseña = reseñaRepository.save(reseña);

        return mapToResponseDTO(reseña);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        Reseña reseña = reseñaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reseña no encontrada"));

        reseña.setDeletedAt(java.time.LocalDateTime.now());
        reseñaRepository.save(reseña);
    }

    private ReseñaResponseDTO mapToResponseDTO(Reseña reseña) {
        return ReseñaResponseDTO.builder()
                .id(reseña.getId())
                .productoId(reseña.getProducto().getId())
                .clienteId(reseña.getCliente().getId())
                .clienteNombre(reseña.getCliente().getNombre())
                .calificacion(reseña.getCalificacion())
                .comentario(reseña.getComentario())
                .aprobada(reseña.getAprobada())
                .creadoEn(reseña.getCreadoEn())
                .build();
    }
}
