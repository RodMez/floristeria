package com.floristeria.floristeria.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class EntregaConfigDTO {

    private Integer sedeId;
    private String horaApertura;
    private String horaCierre;
    private String horaCorte;
    private Integer ventanaMaxDias;
    private Integer leadMinutos;
    private Integer duracionSlotMinutos;
    private List<String> diasNoEntrega;
    private List<String> fechasBloqueadas;
}
