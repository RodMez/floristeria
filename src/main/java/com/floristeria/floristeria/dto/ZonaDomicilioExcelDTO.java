package com.floristeria.floristeria.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ZonaDomicilioExcelDTO {

    private String sedeNombre;
    private String ciudad;
    private String localidad;
    private String barrio;
    private BigDecimal precio;
    private String estado;
}
