package com.floristeria.floristeria.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PedidoAdminResponseDTO {

    private Integer id;
    private String clienteNombre;
    private String clienteEmail;
    private String clienteTelefono;
    private String sedeNombre;
    private String metodoPago;
    private String referenciaPago;
    private DireccionEntregaDTO direccionEntrega;
    private List<DetallePedidoAdminDTO> detalles;
    private BigDecimal total;
    private String estado;
    private String transaccionId;
    private LocalDateTime creadoEn;

    @Getter
    @Setter
    @Builder
    public static class DireccionEntregaDTO {
        private String alias;
        private String direccion;
        private String ciudad;
        private String detalles;
    }

    @Getter
    @Setter
    @Builder
    public static class DetallePedidoAdminDTO {
        private String productoNombre;
        private String productoSku;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private String notaPersonalizacion;
    }
}
