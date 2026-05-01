package com.floristeria.floristeria.service;

import com.floristeria.floristeria.dto.ProductoRequestDTO;
import com.floristeria.floristeria.dto.ProductoResponseDTO;

import java.util.List;

public interface ProductoService {

    List<ProductoResponseDTO> listarTodos();

    ProductoResponseDTO crearProducto(ProductoRequestDTO request);

    ProductoResponseDTO actualizarProducto(Integer id, ProductoRequestDTO request);

    void eliminarProducto(Integer id);
}
