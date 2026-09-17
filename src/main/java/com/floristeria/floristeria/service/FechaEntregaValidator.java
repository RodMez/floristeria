package com.floristeria.floristeria.service;

import com.floristeria.floristeria.entity.Sede;
import com.floristeria.floristeria.exception.FechaEntregaInvalidaException;
import com.floristeria.floristeria.repository.FechaBloqueadaSedeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FechaEntregaValidator {

    public static final int DURACION_SLOT_MINUTOS = 30;

    private final Clock clockBogota;
    private final FechaBloqueadaSedeRepository fechaBloqueadaRepository;

    public void validar(Sede sede, LocalDate fecha, LocalTime hora) {
        if (fecha == null || hora == null) {
            throw new FechaEntregaInvalidaException("La fecha y hora de entrega son obligatorias");
        }

        LocalDateTime now = LocalDateTime.now(clockBogota);
        LocalDate hoy = now.toLocalDate();
        LocalTime horaActual = now.toLocalTime();

        LocalTime apertura = sede.getHoraAperturaEntrega() != null ? sede.getHoraAperturaEntrega() : java.time.LocalTime.of(8, 0);
        LocalTime cierre = sede.getHoraCierreEntrega() != null ? sede.getHoraCierreEntrega() : java.time.LocalTime.of(17, 0);
        LocalTime corte = sede.getHoraCorte() != null ? sede.getHoraCorte() : java.time.LocalTime.of(15, 30);
        int ventanaMax = sede.getVentanaMaxDias() != null ? sede.getVentanaMaxDias() : 30;
        int leadMin = sede.getLeadMinutos() != null ? sede.getLeadMinutos() : 60;

        // Corte inclusivo: now <= 15:30:00 puede entregar hoy
        LocalDate fechaMin = !horaActual.isAfter(corte) ? hoy : hoy.plusDays(1);
        LocalDate fechaMax = hoy.plusDays(ventanaMax);

        if (fecha.isBefore(fechaMin)) {
            if (fecha.isEqual(hoy)) {
                throw new FechaEntregaInvalidaException("Después de las " + formatearHora(corte)
                        + " los pedidos quedan para el día siguiente");
            }
            throw new FechaEntregaInvalidaException("La fecha de entrega no puede ser pasada");
        }
        if (fecha.isAfter(fechaMax)) {
            throw new FechaEntregaInvalidaException("La fecha máxima de reserva es " + ventanaMax + " días");
        }

        if (getDiasNoEntrega(sede).contains(fecha.getDayOfWeek())) {
            throw new FechaEntregaInvalidaException("La sede no entrega ese día de la semana");
        }
        if (sede.getId() != null && fechaBloqueadaRepository.existsBySede_IdAndFecha(sede.getId(), fecha)) {
            throw new FechaEntregaInvalidaException("La fecha seleccionada no está disponible para entrega");
        }

        if (hora.getMinute() != 0 && hora.getMinute() != 30) {
            throw new FechaEntregaInvalidaException("horaEntrega", "Selecciona un horario válido (:00 o :30)");
        }
        if (hora.isBefore(apertura) || hora.isAfter(cierre.minusMinutes(DURACION_SLOT_MINUTOS))) {
            throw new FechaEntregaInvalidaException("horaEntrega",
                    "El horario debe estar entre " + formatearHora(apertura) + " y " + formatearHora(cierre.minusMinutes(DURACION_SLOT_MINUTOS)));
        }

        if (fecha.isEqual(hoy)) {
            LocalTime finSlot = hora.plusMinutes(DURACION_SLOT_MINUTOS);
            LocalTime minimo = horaActual.plusMinutes(leadMin);
            if (!finSlot.isAfter(minimo)) {
                throw new FechaEntregaInvalidaException("horaEntrega",
                        "Elige una hora con al menos 1h de anticipación para preparar tu pedido");
            }
        }
    }

    public Set<DayOfWeek> getDiasNoEntrega(Sede sede) {
        if (sede.getDiasNoEntrega() == null || sede.getDiasNoEntrega().isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(sede.getDiasNoEntrega().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return DayOfWeek.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log.warn("diasNoEntrega con valor invalido '{}' en sede id={} — se ignora (usar MONDAY..SUNDAY)",
                                s, sede.getId());
                        return null;
                    }
                })
                .filter(d -> d != null)
                .collect(Collectors.toSet());
    }

    public boolean esSlotDisponibleHoy(Sede sede, LocalTime inicioSlot) {
        LocalDateTime now = LocalDateTime.now(clockBogota);
        LocalTime horaActual = now.toLocalTime();
        LocalTime corte = sede.getHoraCorte() != null ? sede.getHoraCorte() : java.time.LocalTime.of(15, 30);
        int leadMin = sede.getLeadMinutos() != null ? sede.getLeadMinutos() : 60;
        if (horaActual.isAfter(corte)) {
            return false;
        }
        return inicioSlot.plusMinutes(DURACION_SLOT_MINUTOS).isAfter(horaActual.plusMinutes(leadMin));
    }

    public static String formatearHora(LocalTime t) {
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    public static String etiquetaFranja(LocalTime hora) {
        return hora.getHour() < 12 ? "Mañana" : "Tarde";
    }

    public static String formatearSlot(LocalTime inicio) {
        LocalTime fin = inicio.plusMinutes(DURACION_SLOT_MINUTOS);
        return formatearHora(inicio) + "-" + formatearHora(fin) + " · " + etiquetaFranja(inicio);
    }
}
