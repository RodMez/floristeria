package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.ZonaDomicilioDTO;
import com.floristeria.floristeria.entity.Sede;
import com.floristeria.floristeria.entity.ZonaDomicilio;
import com.floristeria.floristeria.exception.AccesoDenegadoSedeException;
import com.floristeria.floristeria.repository.SedeRepository;
import com.floristeria.floristeria.repository.ZonaDomicilioRepository;
import com.floristeria.floristeria.service.ZonaDomicilioService;
import com.floristeria.floristeria.util.StringUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ZonaDomicilioServiceImpl implements ZonaDomicilioService {

    private final ZonaDomicilioRepository zonaDomicilioRepository;
    private final SedeRepository sedeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ZonaDomicilioDTO.ZonaDomicilioResponseDTO> listarTodas() {
        return zonaDomicilioRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ZonaDomicilioDTO.ZonaDomicilioResponseDTO> listarPorSede(Integer sedeId) {
        return zonaDomicilioRepository.findBySedeId(sedeId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ZonaDomicilioDTO.ZonaDomicilioResponseDTO crear(ZonaDomicilioDTO.ZonaDomicilioRequestDTO request) {
        Sede sede = sedeRepository.findById(request.getSedeId())
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada con id: " + request.getSedeId()));

        String localidad = StringUtil.capitalize(request.getLocalidad());
        String barrio = StringUtil.capitalize(request.getBarrio());

        ZonaDomicilio zona = ZonaDomicilio.builder()
                .sede(sede)
                .localidad(localidad)
                .barrio(barrio)
                .precio(request.getPrecio())
                .build();

        zona = zonaDomicilioRepository.save(zona);
        return toResponseDTO(zona);
    }

    @Override
    public ZonaDomicilioDTO.ZonaDomicilioResponseDTO actualizar(Integer id, ZonaDomicilioDTO.ZonaDomicilioRequestDTO request) {
        ZonaDomicilio zona = zonaDomicilioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Zona de domicilio no encontrada con id: " + id));

        Sede sede = sedeRepository.findById(request.getSedeId())
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada con id: " + request.getSedeId()));

        String localidad = StringUtil.capitalize(request.getLocalidad());
        String barrio = StringUtil.capitalize(request.getBarrio());

        zona.setSede(sede);
        zona.setLocalidad(localidad);
        zona.setBarrio(barrio);
        zona.setPrecio(request.getPrecio());

        zona = zonaDomicilioRepository.save(zona);
        return toResponseDTO(zona);
    }

    @Override
    public void eliminar(Integer id) {
        ZonaDomicilio zona = zonaDomicilioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Zona de domicilio no encontrada con id: " + id));

        zona.setDeletedAt(LocalDateTime.now());
        zonaDomicilioRepository.save(zona);
    }

    @Override
    public ZonaDomicilioDTO.ZonaDomicilioResponseDTO crearConSede(Integer sedeId, ZonaDomicilioDTO.ZonaDomicilioRequestDTO request) {
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada con id: " + sedeId));

        String localidad = StringUtil.capitalize(request.getLocalidad());
        String barrio = StringUtil.capitalize(request.getBarrio());

        ZonaDomicilio zona = ZonaDomicilio.builder()
                .sede(sede)
                .localidad(localidad)
                .barrio(barrio)
                .precio(request.getPrecio())
                .build();

        zona = zonaDomicilioRepository.save(zona);
        return toResponseDTO(zona);
    }

    @Override
    public ZonaDomicilioDTO.ZonaDomicilioResponseDTO actualizarConSede(Integer id, Integer sedeId, ZonaDomicilioDTO.ZonaDomicilioRequestDTO request) {
        ZonaDomicilio zona = zonaDomicilioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Zona de domicilio no encontrada con id: " + id));

        if (!zona.getSede().getId().equals(sedeId)) {
            throw new AccesoDenegadoSedeException("No tiene permisos para modificar zonas de otra sede");
        }

        String localidad = StringUtil.capitalize(request.getLocalidad());
        String barrio = StringUtil.capitalize(request.getBarrio());

        zona.setLocalidad(localidad);
        zona.setBarrio(barrio);
        zona.setPrecio(request.getPrecio());

        zona = zonaDomicilioRepository.save(zona);
        return toResponseDTO(zona);
    }

    @Override
    public void eliminarVerificandoSede(Integer id, Integer sedeId) {
        ZonaDomicilio zona = zonaDomicilioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Zona de domicilio no encontrada con id: " + id));

        if (!zona.getSede().getId().equals(sedeId)) {
            throw new AccesoDenegadoSedeException("No tiene permisos para eliminar zonas de otra sede");
        }

        zona.setDeletedAt(LocalDateTime.now());
        zonaDomicilioRepository.save(zona);
    }

    private ZonaDomicilioDTO.ZonaDomicilioResponseDTO toResponseDTO(ZonaDomicilio zona) {
        return ZonaDomicilioDTO.ZonaDomicilioResponseDTO.builder()
                .id(zona.getId())
                .sedeId(zona.getSede().getId())
                .sedeNombre(zona.getSede().getNombre())
                .localidad(zona.getLocalidad())
                .barrio(zona.getBarrio())
                .precio(zona.getPrecio())
                .build();
    }
}
