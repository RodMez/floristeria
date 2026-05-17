package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.ProductoRequestDTO;
import com.floristeria.floristeria.dto.ProductoResponseDTO;
import com.floristeria.floristeria.entity.Categoria;
import com.floristeria.floristeria.entity.Inventario;
import com.floristeria.floristeria.entity.Producto;
import com.floristeria.floristeria.entity.Sede;
import com.floristeria.floristeria.repository.CategoriaRepository;
import com.floristeria.floristeria.repository.InventarioRepository;
import com.floristeria.floristeria.repository.ProductoRepository;
import com.floristeria.floristeria.repository.SedeRepository;
import com.floristeria.floristeria.service.ProductoService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    
    // inyecciones para la lógica de Inventario Multi-sede
    private final SedeRepository sedeRepository;
    private final InventarioRepository inventarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> listarTodos() {
        return productoRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductoResponseDTO crearProducto(ProductoRequestDTO request) {
        // 1. Buscar la categoría
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Categoría no encontrada con id: " + request.getCategoriaId()));

        // 2. Crear y guardar el Producto Maestro
        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setImagenUrl(request.getImagenUrl());
        producto.setCategoria(categoria);
        producto.setActivoGlobal(true);

        Producto guardado = productoRepository.save(producto);

        // 3. LÓGICA MULTI-TENANT: Repartir el producto a todas las sedes
        List<Sede> todasLasSedes = sedeRepository.findAll();
        List<Inventario> nuevosInventarios = new ArrayList<>();

        for (Sede sede : todasLasSedes) {
            Inventario inventario = new Inventario();
            inventario.setProducto(guardado);
            inventario.setSede(sede);
            inventario.setPrecio(BigDecimal.ZERO); // Precio inicial 0
            inventario.setStock(0);                // Stock inicial 0
            inventario.setDisponible(false);       // Apagado por defecto para que el Admin local lo encienda
            
            nuevosInventarios.add(inventario);
        }

        // 4. Guardar todos los registros de inventario en bloque
        inventarioRepository.saveAll(nuevosInventarios);

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