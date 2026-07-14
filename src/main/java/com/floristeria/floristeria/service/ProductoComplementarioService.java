package com.floristeria.floristeria.service;

import com.floristeria.floristeria.dto.ProductoComplementarioRequestDTO;
import com.floristeria.floristeria.dto.ProductoComplementarioResponseDTO;

import java.util.List;

public interface ProductoComplementarioService {

    List<ProductoComplementarioResponseDTO> listarPorProducto(Integer productoId);

    ProductoComplementarioResponseDTO crear(Integer productoId, ProductoComplementarioRequestDTO request);

    ProductoComplementarioResponseDTO actualizar(Integer id, ProductoComplementarioRequestDTO request);

    void eliminar(Integer id);
}
