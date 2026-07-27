package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.BannerRequestDTO;
import com.floristeria.floristeria.dto.BannerResponseDTO;
import com.floristeria.floristeria.entity.Banner;
import com.floristeria.floristeria.entity.UbicacionBanner;
import com.floristeria.floristeria.exception.ImageKitException;
import com.floristeria.floristeria.repository.BannerRepository;
import com.floristeria.floristeria.service.BannerService;
import com.floristeria.floristeria.service.ImageKitService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final ImageKitService imageKitService;

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponseDTO> listarTodos() {
        return bannerRepository.findAllOrdered().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponseDTO> listarPorUbicacion(String ubicacion, Integer sedeId) {
        validarUbicacion(ubicacion);

        // Regla 3: si hay sede-específicos activos, solo esos; si no, fallback a globales
        if (sedeId != null) {
            List<Banner> especificos = bannerRepository.findActivosByUbicacionAndSedeId(ubicacion, sedeId);
            if (!especificos.isEmpty()) {
                return especificos.stream().map(this::toResponseDTO).toList();
            }
        }

        // Fallback a globales
        return bannerRepository.findActivosByUbicacionGlobal(ubicacion).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BannerResponseDTO obtenerPorId(Integer id) {
        Banner banner = bannerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Banner no encontrado con id: " + id));
        return toResponseDTO(banner);
    }

    @Override
    public BannerResponseDTO crear(BannerRequestDTO request) {
        validarUbicacion(request.getUbicacion());
        validarReglaSelectorSedeGlobal(request.getUbicacion(), request.getSedeId());

        Banner banner = new Banner();
        aplicarRequest(banner, request);

        Banner guardado = bannerRepository.save(banner);
        log.info("Banner creado: id={}, ubicacion={}, sedeId={}", guardado.getId(), guardado.getUbicacion(), guardado.getSedeId());
        return toResponseDTO(guardado);
    }

    @Override
    public BannerResponseDTO actualizar(Integer id, BannerRequestDTO request) {
        validarUbicacion(request.getUbicacion());
        validarReglaSelectorSedeGlobal(request.getUbicacion(), request.getSedeId());

        Banner banner = bannerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Banner no encontrado con id: " + id));

        aplicarRequest(banner, request);
        Banner guardado = bannerRepository.save(banner);
        log.info("Banner actualizado: id={}", guardado.getId());
        return toResponseDTO(guardado);
    }

    @Override
    public void eliminar(Integer id) {
        Banner banner = bannerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Banner no encontrado con id: " + id));

        String imagenUrl = banner.getImagenUrl();
        if (imagenUrl != null && !imagenUrl.isBlank()) {
            try {
                imageKitService.borrar(imagenUrl);
                log.info("Asset de ImageKit borrado: banner={}, url={}", id, imagenUrl);
            } catch (ImageKitException e) {
                log.warn("No se pudo borrar asset en ImageKit (se reintentara en job nocturno): banner={}, url={}, causa={}",
                        id, imagenUrl, e.getMessage());
            }
        }

        banner.setDeletedAt(LocalDateTime.now());
        bannerRepository.save(banner);
        log.info("Banner eliminado (soft): id={}", id);
    }

    // ── Validaciones ────────────────────────────────────────────

    private void validarUbicacion(String ubicacion) {
        try {
            UbicacionBanner.valueOf(ubicacion);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ubicacion invalida: " + ubicacion
                    + ". Valores permitidos: SELECTOR_SEDE, HOME_SEDE, SHOWCASE");
        }
    }

    private void validarReglaSelectorSedeGlobal(String ubicacion, Integer sedeId) {
        if (UbicacionBanner.SELECTOR_SEDE.name().equals(ubicacion) && sedeId != null) {
            throw new IllegalArgumentException("Los banners de SELECTOR_SEDE deben ser globales (sin sede asignada)");
        }
    }

    // ── Mapeo ───────────────────────────────────────────────────

    private void aplicarRequest(Banner banner, BannerRequestDTO request) {
        banner.setSedeId(request.getSedeId());
        banner.setUbicacion(request.getUbicacion());
        banner.setTitulo(request.getTitulo());
        banner.setTexto(request.getTexto());
        banner.setImagenUrl(request.getImagenUrl());
        banner.setEnlaceUrl(request.getEnlaceUrl());
        banner.setOrden(request.getOrden() != null ? request.getOrden() : 0);
        banner.setActivo(request.getActivo() != null ? request.getActivo() : true);
    }

    private BannerResponseDTO toResponseDTO(Banner banner) {
        return BannerResponseDTO.builder()
                .id(banner.getId())
                .sedeId(banner.getSedeId())
                .ubicacion(banner.getUbicacion())
                .titulo(banner.getTitulo())
                .texto(banner.getTexto())
                .imagenUrl(banner.getImagenUrl())
                .enlaceUrl(banner.getEnlaceUrl())
                .orden(banner.getOrden())
                .activo(banner.getActivo())
                .creadoEn(banner.getCreadoEn())
                .actualizadoEn(banner.getActualizadoEn())
                .build();
    }
}
