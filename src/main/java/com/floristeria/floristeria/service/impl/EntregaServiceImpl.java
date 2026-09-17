package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.config.ClockConfig;
import com.floristeria.floristeria.dto.EntregaConfigDTO;
import com.floristeria.floristeria.dto.FechaBloqueadaRequestDTO;
import com.floristeria.floristeria.dto.FechaBloqueadaResponseDTO;
import com.floristeria.floristeria.dto.SlotEntregaDTO;
import com.floristeria.floristeria.entity.FechaBloqueadaSede;
import com.floristeria.floristeria.entity.Sede;
import com.floristeria.floristeria.repository.FechaBloqueadaSedeRepository;
import com.floristeria.floristeria.repository.SedeRepository;
import com.floristeria.floristeria.service.EntregaService;
import com.floristeria.floristeria.service.FechaEntregaValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EntregaServiceImpl implements EntregaService {

    private final SedeRepository sedeRepository;
    private final FechaBloqueadaSedeRepository fechaBloqueadaRepository;
    private final FechaEntregaValidator validator;
    private final Clock clockBogota;

    @Override
    @Transactional(readOnly = true)
    public EntregaConfigDTO obtenerConfig(Integer sedeId) {
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada"));

        LocalDate hoy = LocalDate.now(clockBogota);
        int ventana = sede.getVentanaMaxDias() != null ? sede.getVentanaMaxDias() : 30;
        List<String> bloqueadas = fechaBloqueadaRepository
                .findBySede_IdAndFechaBetween(sedeId, hoy, hoy.plusDays(ventana))
                .stream().map(f -> f.getFecha().toString()).toList();

        return EntregaConfigDTO.builder()
                .sedeId(sede.getId())
                .horaApertura(fmt(sede.getHoraAperturaEntrega(), "08:00"))
                .horaCierre(fmt(sede.getHoraCierreEntrega(), "17:00"))
                .horaCorte(fmt(sede.getHoraCorte(), "15:30"))
                .ventanaMaxDias(ventana)
                .leadMinutos(sede.getLeadMinutos() != null ? sede.getLeadMinutos() : 60)
                .duracionSlotMinutos(FechaEntregaValidator.DURACION_SLOT_MINUTOS)
                .diasNoEntrega(new ArrayList<>(validator.getDiasNoEntrega(sede).stream().map(Enum::name).toList()))
                .fechasBloqueadas(bloqueadas)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlotEntregaDTO> listarSlots(Integer sedeId, LocalDate fecha) {
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada"));
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria (yyyy-MM-dd)");
        }

        LocalTime apertura = sede.getHoraAperturaEntrega() != null ? sede.getHoraAperturaEntrega() : LocalTime.of(8, 0);
        LocalTime cierre = sede.getHoraCierreEntrega() != null ? sede.getHoraCierreEntrega() : LocalTime.of(17, 0);
        int dur = FechaEntregaValidator.DURACION_SLOT_MINUTOS;

        List<SlotEntregaDTO> slots = new ArrayList<>();
        for (LocalTime t = apertura; !t.plusMinutes(dur).isAfter(cierre); t = t.plusMinutes(dur)) {
            String motivo = null;
            boolean disponible = true;
            try {
                // Validación completa: si lanza excepción, el slot no está disponible con ese motivo
                validator.validar(sede, fecha, t);
            } catch (Exception e) {
                disponible = false;
                motivo = e.getMessage();
            }
            LocalTime fin = t.plusMinutes(dur);
            slots.add(SlotEntregaDTO.builder()
                    .inicio(FechaEntregaValidator.formatearHora(t))
                    .fin(FechaEntregaValidator.formatearHora(fin))
                    .etiqueta(FechaEntregaValidator.formatearHora(t) + "-" + FechaEntregaValidator.formatearHora(fin))
                    .franja(FechaEntregaValidator.etiquetaFranja(t))
                    .disponible(disponible)
                    .motivo(motivo)
                    .build());
        }
        return slots;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FechaBloqueadaResponseDTO> listarBloqueos(Integer sedeId) {
        sedeRepository.findById(sedeId).orElseThrow(() -> new EntityNotFoundException("Sede no encontrada"));
        return fechaBloqueadaRepository.findBySede_IdOrderByFechaAsc(sedeId).stream()
                .map(f -> FechaBloqueadaResponseDTO.builder()
                        .id(f.getId()).sedeId(sedeId).fecha(f.getFecha()).motivo(f.getMotivo()).build())
                .toList();
    }

    @Override
    @Transactional
    public FechaBloqueadaResponseDTO agregarBloqueo(Integer sedeId, FechaBloqueadaRequestDTO request) {
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada"));
        if (request.getFecha() == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        if (fechaBloqueadaRepository.existsBySede_IdAndFecha(sedeId, request.getFecha())) {
            throw new IllegalStateException("Esa fecha ya está bloqueada para esta sede");
        }
        FechaBloqueadaSede saved = fechaBloqueadaRepository.save(FechaBloqueadaSede.builder()
                .sede(sede).fecha(request.getFecha()).motivo(request.getMotivo()).build());
        return FechaBloqueadaResponseDTO.builder()
                .id(saved.getId()).sedeId(sedeId).fecha(saved.getFecha()).motivo(saved.getMotivo()).build();
    }

    @Override
    @Transactional
    public void eliminarBloqueo(Integer sedeId, Integer bloqueoId) {
        FechaBloqueadaSede f = fechaBloqueadaRepository.findById(bloqueoId)
                .orElseThrow(() -> new EntityNotFoundException("Bloqueo no encontrado"));
        if (!f.getSede().getId().equals(sedeId)) {
            throw new IllegalArgumentException("El bloqueo no pertenece a esta sede");
        }
        fechaBloqueadaRepository.delete(f);
    }

    private String fmt(LocalTime t, String def) {
        return t != null ? FechaEntregaValidator.formatearHora(t) : def;
    }
}
