package com.floristeria.floristeria.service;

import com.floristeria.floristeria.dto.ZonaDomicilioDTO;

import java.util.List;

public interface ZonaDomicilioService {
    List<ZonaDomicilioDTO.ZonaDomicilioResponseDTO> listarTodas();
    List<ZonaDomicilioDTO.ZonaDomicilioResponseDTO> listarPorSede(Integer sedeId);
    ZonaDomicilioDTO.ZonaDomicilioResponseDTO crear(ZonaDomicilioDTO.ZonaDomicilioRequestDTO request);
    ZonaDomicilioDTO.ZonaDomicilioResponseDTO actualizar(Integer id, ZonaDomicilioDTO.ZonaDomicilioRequestDTO request);
    void eliminar(Integer id);

    ZonaDomicilioDTO.ZonaDomicilioResponseDTO crearConSede(Integer sedeId, ZonaDomicilioDTO.ZonaDomicilioRequestDTO request);
    ZonaDomicilioDTO.ZonaDomicilioResponseDTO actualizarConSede(Integer id, Integer sedeId, ZonaDomicilioDTO.ZonaDomicilioRequestDTO request);
    void eliminarVerificandoSede(Integer id, Integer sedeId);
}
