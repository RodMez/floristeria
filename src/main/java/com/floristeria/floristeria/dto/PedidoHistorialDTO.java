package com.floristeria.floristeria.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PedidoHistorialDTO {

    private String id;
    private BigDecimal total;
    private String estado;
    private LocalDateTime creadoEn;
    private String referenciaPago;
    private String sedeNombre;
    private String metodoPago;
    private DireccionHistorialDTO direccionEntrega;
    private List<DetalleHistorialDTO> detalles;
    private BigDecimal costoEnvio;
    private String zonaDomicilioNombre;
    private String clienteNombre;
    private String clienteEmail;
    private String clienteTelefono;
    private LocalDate fechaEntrega;
    private LocalTime horaEntrega;
    private String franjaEntrega;

    @Getter
    @Setter
    @Builder
    public static class DireccionHistorialDTO {
        private String alias;
        private String direccion;
        private String ciudad;
        private String detalles;
    }

    @Getter
    @Setter
    @Builder
    public static class DetalleHistorialDTO {
        private Integer productoId;
        private String productoNombre;
        private String productoSku;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private String notaPersonalizacion;
    }
}
