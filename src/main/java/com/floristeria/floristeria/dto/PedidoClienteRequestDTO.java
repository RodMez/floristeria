package com.floristeria.floristeria.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoClienteRequestDTO {

    @NotNull(message = "El ID de la sede es obligatorio")
    private Integer sedeId;

    @NotNull(message = "El ID de la dirección es obligatorio")
    private Integer direccionId;

    @NotNull(message = "La lista de detalles es obligatoria")
    @Size(min = 1, message = "Debe haber al menos un producto en el pedido")
    private List<DetallePedidoClienteDTO> detalles;

    private String notasEntrega;

    @NotNull(message = "Debes confirmar la aceptación de los términos y condiciones")
    @AssertTrue(message = "Debes aceptar los términos y condiciones")
    private Boolean aceptaTerminos;
}