package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.entity.ConfiguracionTienda;
import com.floristeria.floristeria.entity.DetallePedido;
import com.floristeria.floristeria.entity.Direccion;
import com.floristeria.floristeria.entity.Pedido;
import com.floristeria.floristeria.entity.ZonaDomicilio;
import com.floristeria.floristeria.service.ConfiguracionTiendaService;
import com.floristeria.floristeria.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final RestTemplate restTemplate;
    private final ConfiguracionTiendaService configuracionService;

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    @Value("${brevo.sender-email}")
    private String brevoSenderEmail;

    @Value("${brevo.sender-name}")
    private String brevoSenderName;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Async
    @Override
    public void notificarNuevaVenta(Pedido pedido) {
        try {
            // 1. Correo al Cliente (Recibo de compra)
            if (pedido.getCliente() != null && pedido.getCliente().getEmail() != null) {
                String asuntoCliente = "Recibo de tu compra - Pedido #" + pedido.getId();
                String htmlCliente = construirHtmlReciboCliente(pedido);
                enviarCorreoBrevo(pedido.getCliente().getEmail(), pedido.getCliente().getNombre(),
                        asuntoCliente, htmlCliente);
            }

            // 2. Correo a la Sede (Nueva venta)
            if (pedido.getSede() != null && pedido.getSede().getEmail() != null
                    && !pedido.getSede().getEmail().isBlank()) {
                String asuntoSede = "Nueva venta - Pedido #" + pedido.getId();
                String htmlSede = construirHtmlNuevaVenta(pedido);
                enviarCorreoBrevo(pedido.getSede().getEmail(), pedido.getSede().getNombre(),
                        asuntoSede, htmlSede);
            }

            // 3. Copia al Correo Maestro (si está habilitado)
            ConfiguracionTienda config = configuracionService.obtenerConfiguracion();
            if (Boolean.TRUE.equals(config.getEnviarCopiaMaestro())
                    && config.getCorreoMaestro() != null
                    && !config.getCorreoMaestro().isBlank()) {
                String asuntoMaestro = "Copia de venta - Pedido #" + pedido.getId();
                String htmlMaestro = construirHtmlCopiaMaestro(pedido);
                enviarCorreoBrevo(config.getCorreoMaestro(), "Administrador",
                        asuntoMaestro, htmlMaestro);
            }

        } catch (Exception e) {
            log.error("Error al enviar notificación de venta para pedido #{}: {}",
                    pedido.getId(), e.getMessage());
        }
    }

    private void enviarCorreoBrevo(String toEmail, String toName, String subject, String htmlContent) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        Map<String, Object> sender = Map.of(
                "email", brevoSenderEmail,
                "name", brevoSenderName
        );

        Map<String, String> to = Map.of(
                "email", toEmail,
                "name", toName != null ? toName : ""
        );

        Map<String, Object> body = Map.of(
                "sender", sender,
                "to", List.of(to),
                "subject", subject,
                "htmlContent", htmlContent
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                BREVO_API_URL, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Brevo API respondió con estado {} para destinatario {}",
                    response.getStatusCode(), toEmail);
        }
    }

    private String construirHtmlReciboCliente(Pedido pedido) {
        StringBuilder detallesHtml = new StringBuilder();
        if (pedido.getDetalles() != null) {
            for (DetallePedido detalle : pedido.getDetalles()) {
                String nombreProducto = detalle.getProducto() != null
                        ? detalle.getProducto().getNombre() : "Producto eliminado";
                String nota = detalle.getNotaPersonalizacion() != null
                        ? detalle.getNotaPersonalizacion() : "N/A";
                detallesHtml.append(String.format(
                        "<tr><td style='padding:8px;border:1px solid #ddd;'>%s</td>" +
                        "<td style='padding:8px;border:1px solid #ddd;text-align:center;'>%d</td>" +
                        "<td style='padding:8px;border:1px solid #ddd;text-align:right;'>$%,.0f</td>" +
                        "<td style='padding:8px;border:1px solid #ddd;'>%s</td></tr>",
                        nombreProducto, detalle.getCantidad(), detalle.getPrecioUnitario(), nota));
            }
        }

        String fechaFormateada = pedido.getCreadoEn() != null
                ? pedido.getCreadoEn().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "Fecha no disponible";

        String direccionHtml;
        if (pedido.getDireccion() != null) {
            Direccion d = pedido.getDireccion();
            String detalles = d.getDetalles() != null ? d.getDetalles() : "Sin detalles adicionales";
            direccionHtml = """
                    <h3>Dirección de entrega:</h3>
                    <ul style="list-style:none;padding:0;">
                        <li><strong>Alias:</strong> %s</li>
                        <li><strong>Dirección:</strong> %s</li>
                        <li><strong>Ciudad:</strong> %s</li>
                        <li><strong>Detalles:</strong> %s</li>
                    </ul>
                    """.formatted(d.getAlias(), d.getDireccion(), d.getCiudad(), detalles);
        } else {
            direccionHtml = "<p><em>Dirección no disponible</em></p>";
        }

        String notasEntregaHtml = "";
        if (pedido.getNotasEntrega() != null && !pedido.getNotasEntrega().isBlank()) {
            notasEntregaHtml = "<p><strong>Notas de entrega:</strong> " + pedido.getNotasEntrega() + "</p>";
        }

        String zonaEnvio = "N/A";
        if (pedido.getDireccion() != null && pedido.getDireccion().getZonaDomicilio() != null) {
            ZonaDomicilio zona = pedido.getDireccion().getZonaDomicilio();
            zonaEnvio = zona.getLocalidad()
                    + (zona.getBarrio() != null ? " - " + zona.getBarrio() : "");
        }
        BigDecimal costoEnvio = pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : BigDecimal.ZERO;

        return """
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                    <div style="background-color:#4CAF50;color:white;padding:20px;text-align:center;">
                        <h1>Floristeria - Confirmación de Pedido</h1>
                    </div>
                    <div style="padding:20px;">
                        <p>Hola <strong>%s</strong>,</p>
                        <p>Tu pedido ha sido confirmado y el pago procesado exitosamente.</p>
                        <p><strong>Referencia de pago:</strong> %s</p>
                        <p><strong>Fecha:</strong> %s</p>
                        %s
                        %s
                        <h3>Detalles del pedido:</h3>
                        <table style="width:100%%;border-collapse:collapse;">
                            <thead>
                                <tr style="background-color:#f2f2f2;">
                                    <th style="padding:8px;border:1px solid #ddd;text-align:left;">Producto</th>
                                    <th style="padding:8px;border:1px solid #ddd;text-align:center;">Cantidad</th>
                                    <th style="padding:8px;border:1px solid #ddd;text-align:right;">Precio</th>
                                    <th style="padding:8px;border:1px solid #ddd;text-align:left;">Notas</th>
                                </tr>
                            </thead>
                            <tbody>%s</tbody>
                        </table>
                        <hr style="margin:20px 0;">
                        <p style="text-align:right;color:#555;">Costo de Envío (Zona: %s): $%,.0f</p>
                        <p style="text-align:right;font-size:18px;"><strong>Total: $%,.0f</strong></p>
                        <p style="color:#666;">Método de pago: %s</p>
                    </div>
                    <div style="background-color:#f2f2f2;padding:10px;text-align:center;color:#666;">
                        <p>Gracias por tu compra</p>
                    </div>
                </body>
                </html>
                """.formatted(
                pedido.getCliente().getNombre(),
                pedido.getReferenciaPago() != null ? pedido.getReferenciaPago() : "N/A",
                fechaFormateada,
                direccionHtml,
                notasEntregaHtml,
                detallesHtml,
                zonaEnvio, costoEnvio,
                pedido.getTotal(),
                pedido.getMetodoPago() != null ? pedido.getMetodoPago() : "No especificado"
        );
    }

    private String construirHtmlNuevaVenta(Pedido pedido) {
        String clienteNombre = pedido.getCliente() != null ? pedido.getCliente().getNombre() : "N/A";
        String clienteEmail = pedido.getCliente() != null ? pedido.getCliente().getEmail() : "N/A";
        String clienteTelefono = pedido.getCliente() != null ? pedido.getCliente().getTelefono() : "N/A";

        StringBuilder detallesHtml = new StringBuilder();
        if (pedido.getDetalles() != null) {
            for (DetallePedido detalle : pedido.getDetalles()) {
                String nombreProducto = detalle.getProducto() != null
                        ? detalle.getProducto().getNombre() : "Producto eliminado";
                String nota = detalle.getNotaPersonalizacion() != null
                        ? detalle.getNotaPersonalizacion() : "N/A";
                detallesHtml.append(String.format(
                        "<tr><td style='padding:8px;border:1px solid #ddd;'>%s</td>" +
                        "<td style='padding:8px;border:1px solid #ddd;text-align:center;'>%d</td>" +
                        "<td style='padding:8px;border:1px solid #ddd;text-align:right;'>$%,.0f</td>" +
                        "<td style='padding:8px;border:1px solid #ddd;'>%s</td></tr>",
                        nombreProducto, detalle.getCantidad(), detalle.getPrecioUnitario(), nota));
            }
        }

        String fechaFormateada = pedido.getCreadoEn() != null
                ? pedido.getCreadoEn().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "Fecha no disponible";

        String direccionHtml;
        if (pedido.getDireccion() != null) {
            Direccion d = pedido.getDireccion();
            String detalles = d.getDetalles() != null ? d.getDetalles() : "Sin detalles adicionales";
            direccionHtml = """
                    <h3>Dirección de entrega:</h3>
                    <ul style="list-style:none;padding:0;">
                        <li><strong>Alias:</strong> %s</li>
                        <li><strong>Dirección:</strong> %s</li>
                        <li><strong>Ciudad:</strong> %s</li>
                        <li><strong>Detalles:</strong> %s</li>
                    </ul>
                    """.formatted(d.getAlias(), d.getDireccion(), d.getCiudad(), detalles);
        } else {
            direccionHtml = "<p><em>Dirección no disponible</em></p>";
        }

        String notasEntregaHtml = "";
        if (pedido.getNotasEntrega() != null && !pedido.getNotasEntrega().isBlank()) {
            notasEntregaHtml = "<p><strong>Notas de entrega:</strong> " + pedido.getNotasEntrega() + "</p>";
        }

        String zonaEnvio = "N/A";
        if (pedido.getDireccion() != null && pedido.getDireccion().getZonaDomicilio() != null) {
            ZonaDomicilio zona = pedido.getDireccion().getZonaDomicilio();
            zonaEnvio = zona.getLocalidad()
                    + (zona.getBarrio() != null ? " - " + zona.getBarrio() : "");
        }
        BigDecimal costoEnvio = pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : BigDecimal.ZERO;

        return """
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                    <div style="background-color:#2196F3;color:white;padding:20px;text-align:center;">
                        <h1>Nueva Venta - %s</h1>
                    </div>
                    <div style="padding:20px;">
                        <p>Se ha registrado una <strong>nueva venta</strong> en tu sede.</p>
                        <h3>Referencia: %s</h3>
                        <p><strong>Fecha:</strong> %s</p>
                        <ul style="list-style:none;padding:0;">
                            <li><strong>Cliente:</strong> %s</li>
                            <li><strong>Email:</strong> %s</li>
                            <li><strong>Teléfono:</strong> %s</li>
                            <li><strong>Método de pago:</strong> %s</li>
                        </ul>
                        %s
                        %s
                        <h3>Detalles:</h3>
                        <table style="width:100%%;border-collapse:collapse;">
                            <thead>
                                <tr style="background-color:#f2f2f2;">
                                    <th style="padding:8px;border:1px solid #ddd;text-align:left;">Producto</th>
                                    <th style="padding:8px;border:1px solid #ddd;text-align:center;">Cantidad</th>
                                    <th style="padding:8px;border:1px solid #ddd;text-align:right;">Precio</th>
                                    <th style="padding:8px;border:1px solid #ddd;text-align:left;">Notas</th>
                                </tr>
                            </thead>
                            <tbody>%s</tbody>
                        </table>
                        <hr style="margin:20px 0;">
                        <p style="text-align:right;color:#555;">Costo de Envío (Zona: %s): $%,.0f</p>
                        <p style="text-align:right;font-size:18px;"><strong>Total: $%,.0f</strong></p>
                    </div>
                </body>
                </html>
                """.formatted(
                pedido.getSede().getNombre(),
                pedido.getReferenciaPago() != null ? pedido.getReferenciaPago() : "N/A",
                fechaFormateada,
                clienteNombre,
                clienteEmail,
                clienteTelefono,
                pedido.getMetodoPago() != null ? pedido.getMetodoPago() : "No especificado",
                direccionHtml,
                notasEntregaHtml,
                detallesHtml,
                zonaEnvio, costoEnvio,
                pedido.getTotal()
        );
    }

    private String construirHtmlCopiaMaestro(Pedido pedido) {
        String clienteNombre = pedido.getCliente() != null ? pedido.getCliente().getNombre() : "N/A";
        String clienteEmail = pedido.getCliente() != null ? pedido.getCliente().getEmail() : "N/A";
        String clienteTelefono = pedido.getCliente() != null ? pedido.getCliente().getTelefono() : "N/A";

        StringBuilder detallesHtml = new StringBuilder();
        if (pedido.getDetalles() != null) {
            for (DetallePedido detalle : pedido.getDetalles()) {
                String nombreProducto = detalle.getProducto() != null
                        ? detalle.getProducto().getNombre() : "Producto eliminado";
                String nota = detalle.getNotaPersonalizacion() != null
                        ? detalle.getNotaPersonalizacion() : "N/A";
                detallesHtml.append(String.format(
                        "<tr><td style='padding:8px;border:1px solid #ddd;'>%s</td>" +
                        "<td style='padding:8px;border:1px solid #ddd;text-align:center;'>%d</td>" +
                        "<td style='padding:8px;border:1px solid #ddd;text-align:right;'>$%,.0f</td>" +
                        "<td style='padding:8px;border:1px solid #ddd;'>%s</td></tr>",
                        nombreProducto, detalle.getCantidad(), detalle.getPrecioUnitario(), nota));
            }
        }

        String fechaFormateada = pedido.getCreadoEn() != null
                ? pedido.getCreadoEn().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "Fecha no disponible";

        String direccionHtml;
        if (pedido.getDireccion() != null) {
            Direccion d = pedido.getDireccion();
            String detalles = d.getDetalles() != null ? d.getDetalles() : "Sin detalles adicionales";
            direccionHtml = """
                    <h3>Dirección de entrega:</h3>
                    <ul style="list-style:none;padding:0;">
                        <li><strong>Alias:</strong> %s</li>
                        <li><strong>Dirección:</strong> %s</li>
                        <li><strong>Ciudad:</strong> %s</li>
                        <li><strong>Detalles:</strong> %s</li>
                    </ul>
                    """.formatted(d.getAlias(), d.getDireccion(), d.getCiudad(), detalles);
        } else {
            direccionHtml = "<p><em>Dirección no disponible</em></p>";
        }

        String notasEntregaHtml = "";
        if (pedido.getNotasEntrega() != null && !pedido.getNotasEntrega().isBlank()) {
            notasEntregaHtml = "<p><strong>Notas de entrega:</strong> " + pedido.getNotasEntrega() + "</p>";
        }

        String zonaEnvio = "N/A";
        if (pedido.getDireccion() != null && pedido.getDireccion().getZonaDomicilio() != null) {
            ZonaDomicilio zona = pedido.getDireccion().getZonaDomicilio();
            zonaEnvio = zona.getLocalidad()
                    + (zona.getBarrio() != null ? " - " + zona.getBarrio() : "");
        }
        BigDecimal costoEnvio = pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : BigDecimal.ZERO;

        return """
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                    <div style="background-color:#9C27B0;color:white;padding:20px;text-align:center;">
                        <h1>Copia de Venta - Referencia: %s</h1>
                    </div>
                    <div style="padding:20px;">
                        <p>Se ha registrado una <strong>nueva venta</strong> en el sistema.</p>
                        <h3>Referencia: %s</h3>
                        <p><strong>Fecha:</strong> %s</p>
                        <ul style="list-style:none;padding:0;">
                            <li><strong>Cliente:</strong> %s</li>
                            <li><strong>Email:</strong> %s</li>
                            <li><strong>Teléfono:</strong> %s</li>
                            <li><strong>Método de pago:</strong> %s</li>
                        </ul>
                        %s
                        %s
                        <h3>Detalles:</h3>
                        <table style="width:100%%;border-collapse:collapse;">
                            <thead>
                                <tr style="background-color:#f2f2f2;">
                                    <th style="padding:8px;border:1px solid #ddd;text-align:left;">Producto</th>
                                    <th style="padding:8px;border:1px solid #ddd;text-align:center;">Cantidad</th>
                                    <th style="padding:8px;border:1px solid #ddd;text-align:right;">Precio</th>
                                    <th style="padding:8px;border:1px solid #ddd;text-align:left;">Notas</th>
                                </tr>
                            </thead>
                            <tbody>%s</tbody>
                        </table>
                        <hr style="margin:20px 0;">
                        <p style="text-align:right;color:#555;">Costo de Envío (Zona: %s): $%,.0f</p>
                        <p style="text-align:right;font-size:18px;"><strong>Total: $%,.0f</strong></p>
                    </div>
                </body>
                </html>
                """.formatted(
                pedido.getReferenciaPago() != null ? pedido.getReferenciaPago() : "N/A",
                pedido.getReferenciaPago() != null ? pedido.getReferenciaPago() : "N/A",
                fechaFormateada,
                clienteNombre,
                clienteEmail,
                clienteTelefono,
                pedido.getMetodoPago() != null ? pedido.getMetodoPago() : "No especificado",
                direccionHtml,
                notasEntregaHtml,
                detallesHtml,
                zonaEnvio, costoEnvio,
                pedido.getTotal()
        );
    }
}
