package com.floristeria.floristeria.service;

import com.floristeria.floristeria.dto.EntregaConfigDTO;
import com.floristeria.floristeria.dto.FechaBloqueadaRequestDTO;
import com.floristeria.floristeria.dto.FechaBloqueadaResponseDTO;
import com.floristeria.floristeria.dto.SlotEntregaDTO;

import java.time.LocalDate;
import java.util.List;

public interface EntregaService {

    EntregaConfigDTO obtenerConfig(Integer sedeId);

    List<SlotEntregaDTO> listarSlots(Integer sedeId, LocalDate fecha);

    List<FechaBloqueadaResponseDTO> listarBloqueos(Integer sedeId);

    FechaBloqueadaResponseDTO agregarBloqueo(Integer sedeId, FechaBloqueadaRequestDTO request);

    void eliminarBloqueo(Integer sedeId, Integer bloqueoId);
}
