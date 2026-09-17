package com.floristeria.floristeria.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SlotEntregaDTO {

    private String inicio;
    private String fin;
    private String etiqueta;
    private String franja;
    private boolean disponible;
    private String motivo;
}
