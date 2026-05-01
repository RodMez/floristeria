package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.ProductoRequestDTO;
import com.floristeria.floristeria.dto.ProductoResponseDTO;
import com.floristeria.floristeria.entity.Categoria;
import com.floristeria.floristeria.entity.Producto;
import com.floristeria.floristeria.repository.CategoriaRepository;
import com.floristeria.floristeria.repository.ProductoRepository;
import com.floristeria.floristeria.service.ProductoService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> listarTodos() {
        return productoRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProductoResponseDTO crearProducto(ProductoRequestDTO request) {
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Categoría no encontrada con id: " + request.getCategoriaId()));

        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setImagenUrl(request.getImagenUrl());
        producto.setCategoria(categoria);
        producto.setActivoGlobal(true);

        Producto guardado = productoRepository.save(producto);
        return toResponseDTO(guardado);
    }

    @Override
    public ProductoResponseDTO actualizarProducto(Integer id, ProductoRequestDTO request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));

        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Categoría no encontrada con id: " + request.getCategoriaId()));

        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setImagenUrl(request.getImagenUrl());
        producto.setCategoria(categoria);

        Producto guardado = productoRepository.save(producto);
        return toResponseDTO(guardado);
    }

    @Override
    public void eliminarProducto(Integer id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));

        productoRepository.delete(producto);
    }

    private ProductoResponseDTO toResponseDTO(Producto producto) {
        return ProductoResponseDTO.builder()
                .id(producto.getId())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .imagenUrl(producto.getImagenUrl())
                .categoriaId(producto.getCategoria().getId())
                .categoriaNombre(producto.getCategoria().getNombre())
                .build();
    }
}
