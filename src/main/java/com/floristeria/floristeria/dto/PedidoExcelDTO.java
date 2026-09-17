package com.floristeria.floristeria.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PedidoExcelDTO {

    private String codigo;
    private LocalDateTime creadoEn;
    private String clienteNombre;
    private String clienteTelefono;
    private String clienteEmail;
    private String sedeNombre;
    private BigDecimal total;
    private BigDecimal costoEnvio;
    private String estado;
    private String metodoPago;
    private String referenciaPago;
    private String direccionEntrega;
    private String notasEntrega;
    private LocalDate fechaEntrega;
    private LocalTime horaEntrega;
    private String franjaEntrega;
}
