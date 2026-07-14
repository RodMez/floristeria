package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.ProductoComplementarioRequestDTO;
import com.floristeria.floristeria.dto.ProductoComplementarioResponseDTO;
import com.floristeria.floristeria.entity.Producto;
import com.floristeria.floristeria.entity.ProductoComplementario;
import com.floristeria.floristeria.entity.Sede;
import com.floristeria.floristeria.repository.ProductoComplementarioRepository;
import com.floristeria.floristeria.repository.ProductoRepository;
import com.floristeria.floristeria.repository.SedeRepository;
import com.floristeria.floristeria.service.ProductoComplementarioService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductoComplementarioServiceImpl implements ProductoComplementarioService {

    private final ProductoComplementarioRepository repository;
    private final ProductoRepository productoRepository;
    private final SedeRepository sedeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductoComplementarioResponseDTO> listarPorProducto(Integer productoId) {
        return repository.findByProductoIdOrderByOrdenAsc(productoId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProductoComplementarioResponseDTO crear(Integer productoId, ProductoComplementarioRequestDTO request) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + productoId));

        Producto complementario = productoRepository.findById(request.getComplementarioId())
                .orElseThrow(() -> new EntityNotFoundException("Producto complementario no encontrado con id: " + request.getComplementarioId()));

        if (producto.getId().equals(complementario.getId())) {
            throw new IllegalArgumentException("Un producto no puede ser complemento de sí mismo");
        }

        if (repository.existsByProductoIdAndComplementarioId(productoId, request.getComplementarioId())) {
            throw new IllegalArgumentException("Esta relación de complemento ya existe");
        }

        Sede sede = null;
        if (request.getSedeId() != null) {
            sede = sedeRepository.findById(request.getSedeId())
                    .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada con id: " + request.getSedeId()));
        }

        ProductoComplementario pc = ProductoComplementario.builder()
                .producto(producto)
                .complementario(complementario)
                .sede(sede)
                .orden(request.getOrden() != null ? request.getOrden() : 0)
                .build();

        ProductoComplementario guardado = repository.save(pc);
        return toResponseDTO(guardado);
    }

    @Override
    public ProductoComplementarioResponseDTO actualizar(Integer id, ProductoComplementarioRequestDTO request) {
        ProductoComplementario pc = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Relación de complemento no encontrada con id: " + id));

        if (request.getComplementarioId() != null && !request.getComplementarioId().equals(pc.getComplementario().getId())) {
            Producto complementario = productoRepository.findById(request.getComplementarioId())
                    .orElseThrow(() -> new EntityNotFoundException("Producto complementario no encontrado"));
            pc.setComplementario(complementario);
        }

        if (request.getSedeId() != null) {
            Sede sede = sedeRepository.findById(request.getSedeId())
                    .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada"));
            pc.setSede(sede);
        } else {
            pc.setSede(null);
        }

        if (request.getOrden() != null) {
            pc.setOrden(request.getOrden());
        }

        ProductoComplementario actualizado = repository.save(pc);
        return toResponseDTO(actualizado);
    }

    @Override
    public void eliminar(Integer id) {
        ProductoComplementario pc = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Relación de complemento no encontrada con id: " + id));
        repository.delete(pc);
    }

    private ProductoComplementarioResponseDTO toResponseDTO(ProductoComplementario pc) {
        return ProductoComplementarioResponseDTO.builder()
                .id(pc.getId())
                .complementarioId(pc.getComplementario().getId())
                .complementarioNombre(pc.getComplementario().getNombre())
                .complementarioImagenUrl(pc.getComplementario().getImagenUrl())
                .sedeId(pc.getSede() != null ? pc.getSede().getId() : null)
                .sedeNombre(pc.getSede() != null ? pc.getSede().getNombre() : null)
                .orden(pc.getOrden())
                .build();
    }
}
