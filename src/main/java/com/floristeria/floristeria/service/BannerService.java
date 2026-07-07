package com.floristeria.floristeria.service;

import com.floristeria.floristeria.dto.BannerRequestDTO;
import com.floristeria.floristeria.dto.BannerResponseDTO;

import java.util.List;

public interface BannerService {
    List<BannerResponseDTO> listarTodos();
    List<BannerResponseDTO> listarPorUbicacion(String ubicacion, Integer sedeId);
    BannerResponseDTO obtenerPorId(Integer id);
    BannerResponseDTO crear(BannerRequestDTO request);
    BannerResponseDTO actualizar(Integer id, BannerRequestDTO request);
    void eliminar(Integer id);
}
