package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.SedeRequestDTO;
import com.floristeria.floristeria.dto.SedeResponseDTO;
import com.floristeria.floristeria.entity.Sede;
import com.floristeria.floristeria.repository.SedeRepository;
import com.floristeria.floristeria.service.SedeService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SedeServiceImpl implements SedeService {

    private final SedeRepository sedeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SedeResponseDTO> listarTodas() {
        return sedeRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SedeResponseDTO crearSede(SedeRequestDTO requestDTO) {
        Sede sede = new Sede();
        sede.setNombre(requestDTO.getNombre());
        sede.setCiudad(requestDTO.getCiudad());
        sede.setWhatsapp(requestDTO.getTelefonoWhatsapp());

        Sede sedeGuardada = sedeRepository.save(sede);
        return toResponseDTO(sedeGuardada);
    }

    @Override
    public SedeResponseDTO actualizarSede(Integer id, SedeRequestDTO requestDTO) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada con id: " + id));

        sede.setNombre(requestDTO.getNombre());
        sede.setCiudad(requestDTO.getCiudad());
        sede.setWhatsapp(requestDTO.getTelefonoWhatsapp());

        Sede sedeActualizada = sedeRepository.save(sede);
        return toResponseDTO(sedeActualizada);
    }

    @Override
    public void eliminarSede(Integer id) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada con id: " + id));
        sedeRepository.delete(sede);
    }

    private SedeResponseDTO toResponseDTO(Sede sede) {
        return SedeResponseDTO.builder()
                .id(sede.getId())
                .nombre(sede.getNombre())
                .ciudad(sede.getCiudad())
                .telefonoWhatsapp(sede.getWhatsapp())
                .build();
    }
}
