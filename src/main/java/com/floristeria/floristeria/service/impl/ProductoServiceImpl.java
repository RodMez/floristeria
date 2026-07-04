package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.ProductoRequestDTO;
import com.floristeria.floristeria.dto.ProductoResponseDTO;
import com.floristeria.floristeria.entity.Categoria;
import com.floristeria.floristeria.entity.EstadoPedido;
import com.floristeria.floristeria.entity.Inventario;
import com.floristeria.floristeria.entity.Producto;
import com.floristeria.floristeria.entity.Sede;
import com.floristeria.floristeria.repository.CategoriaRepository;
import com.floristeria.floristeria.repository.DetallePedidoRepository;
import com.floristeria.floristeria.repository.InventarioRepository;
import com.floristeria.floristeria.repository.ProductoRepository;
import com.floristeria.floristeria.repository.SedeRepository;
import com.floristeria.floristeria.service.ProductoService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    
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
        // 1. Buscar todas las categorías por sus IDs
        List<Categoria> categorias = categoriaRepository.findAllById(request.getCategoriaIds());

        if (categorias.size() != request.getCategoriaIds().size()) {
            throw new EntityNotFoundException("Una o más categorías no encontradas");
        }

        // 2. Determinar SKU: auto-generar si viene vacío
        String sku = (request.getSku() != null && !request.getSku().isBlank())
                ? request.getSku().trim()
                : "PRD-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        if (productoRepository.findBySku(sku).isPresent()) {
            throw new IllegalArgumentException("Ya existe un producto con el SKU: " + sku);
        }

        // 3. Crear y guardar el Producto Maestro
        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setImagenUrl(request.getImagenUrl());
        producto.setSku(sku);
        producto.setCategorias(categorias);
        producto.setActivoGlobal(true);

        Producto guardado = productoRepository.save(producto);

        // 4. LÓGICA MULTI-TENANT: Repartir el producto a todas las sedes
        List<Sede> todasLasSedes = sedeRepository.findAll();
        List<Inventario> nuevosInventarios = new ArrayList<>();

        for (Sede sede : todasLasSedes) {
            Inventario inventario = new Inventario();
            inventario.setProducto(guardado);
            inventario.setSede(sede);
            inventario.setPrecio(BigDecimal.ZERO); // Precio inicial 0
            inventario.setStock(0);                // Stock inicial 0
            inventario.setDisponible(false);       // Apagado por defecto para que el Admin local lo encienda
            inventario.setDescuentoPorcentaje(0);

            nuevosInventarios.add(inventario);
        }

        // 5. Guardar todos los registros de inventario en bloque
        inventarioRepository.saveAll(nuevosInventarios);

        return toResponseDTO(guardado);
    }

    @Override
    public ProductoResponseDTO actualizarProducto(Integer id, ProductoRequestDTO request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));

        List<Categoria> categorias = categoriaRepository.findAllById(request.getCategoriaIds());

        if (categorias.size() != request.getCategoriaIds().size()) {
            throw new EntityNotFoundException("Una o más categorías no encontradas");
        }

        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setImagenUrl(request.getImagenUrl());

        if (request.getSku() != null && !request.getSku().isBlank()
                && !request.getSku().trim().equals(producto.getSku())) {
            String nuevoSku = request.getSku().trim();
            productoRepository.findBySku(nuevoSku).ifPresent(p -> {
                if (!p.getId().equals(producto.getId())) {
                    throw new IllegalArgumentException("Ya existe un producto con el SKU: " + nuevoSku);
                }
            });
            producto.setSku(nuevoSku);
        }

        producto.setCategorias(categorias);

        Producto guardado = productoRepository.save(producto);
        return toResponseDTO(guardado);
    }

    @Override
    public void eliminarProducto(Integer id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));

        List<EstadoPedido> estadosFinales = List.of(EstadoPedido.CANCELADO, EstadoPedido.ENTREGADO);
        boolean tienePedidosActivos = detallePedidoRepository
                .existsByProducto_IdAndPedido_EstadoNotIn(id, estadosFinales);

        if (tienePedidosActivos) {
            throw new IllegalStateException(
                    "No se puede eliminar el producto porque tiene pedidos activos asociados.");
        }

        boolean estaDisponibleEnAlgunaSede = inventarioRepository
                .existsByProducto_IdAndDisponibleTrueAndStockGreaterThan(id, 0);

        if (estaDisponibleEnAlgunaSede) {
            throw new IllegalStateException(
                    "No se puede eliminar el producto porque está disponible en alguna sede. Desactívalo primero.");
        }
                
        producto.setDeletedAt(LocalDateTime.now());
        productoRepository.save(producto);
    }

    private ProductoResponseDTO toResponseDTO(Producto producto) {
        List<ProductoResponseDTO.CategoriaInfo> categoriaInfos = producto.getCategorias().stream()
                .map(cat -> ProductoResponseDTO.CategoriaInfo.builder()
                        .id(cat.getId())
                        .nombre(cat.getNombre())
                        .build())
                .collect(Collectors.toList());

        return ProductoResponseDTO.builder()
                .id(producto.getId())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .imagenUrl(producto.getImagenUrl())
                .sku(producto.getSku())
                .categorias(categoriaInfos)
                .build();
    }
}