package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.entity.ConfiguracionTienda;
import com.floristeria.floristeria.entity.DetallePedido;
import com.floristeria.floristeria.entity.Pedido;
import com.floristeria.floristeria.service.ConfiguracionTiendaService;
import com.floristeria.floristeria.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
                detallesHtml.append(String.format(
                        "<tr><td style='padding:8px;border:1px solid #ddd;'>%s</td>" +
                        "<td style='padding:8px;border:1px solid #ddd;text-align:center;'>%d</td>" +
                        "<td style='padding:8px;border:1px solid #ddd;text-align:right;'>$%,.0f</td></tr>",
                        nombreProducto, detalle.getCantidad(), detalle.getPrecioUnitario()));
            }
        }

        return """
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                    <div style="background-color:#4CAF50;color:white;padding:20px;text-align:center;">
                        <h1>Floristeria - Confirmación de Pedido</h1>
                    </div>
                    <div style="padding:20px;">
                        <p>Hola <strong>%s</strong>,</p>
                        <p>Tu pedido <strong>#%d</strong> ha sido confirmado y el pago procesado exitosamente.</p>
                        <h3>Detalles del pedido:</h3>
                        <table style="width:100%%;border-collapse:collapse;">
                            <thead>
                                <tr style="background-color:#f2f2f2;">
                                    <th style="padding:8px;border:1px solid #ddd;text-align:left;">Producto</th>
                                    <th style="padding:8px;border:1px solid #ddd;text-align:center;">Cantidad</th>
                                    <th style="padding:8px;border:1px solid #ddd;text-align:right;">Precio</th>
                                </tr>
                            </thead>
                            <tbody>%s</tbody>
                        </table>
                        <hr style="margin:20px 0;">
                        <p style="text-align:right;font-size:18px;"><strong>Total: $%,.0f</strong></p>
                        <p style="color:#666;">Método de pago: %s</p>
                        <p style="color:#666;">Referencia: %s</p>
                    </div>
                    <div style="background-color:#f2f2f2;padding:10px;text-align:center;color:#666;">
                        <p>Gracias por tu compra</p>
                    </div>
                </body>
                </html>
                """.formatted(
                pedido.getCliente().getNombre(),
                pedido.getId(),
                detallesHtml,
                pedido.getTotal(),
                pedido.getMetodoPago() != null ? pedido.getMetodoPago() : "No especificado",
                pedido.getReferenciaPago() != null ? pedido.getReferenciaPago() : "N/A"
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
                detallesHtml.append(String.format(
                        "<tr><td style='padding:8px;border:1px solid #ddd;'>%s</td>" +
                        "<td style='padding:8px;border:1px solid #ddd;text-align:center;'>%d</td>" +
                        "<td style='padding:8px;border:1px solid #ddd;text-align:right;'>$%,.0f</td></tr>",
                        nombreProducto, detalle.getCantidad(), detalle.getPrecioUnitario()));
            }
        }

        return """
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                    <div style="background-color:#2196F3;color:white;padding:20px;text-align:center;">
                        <h1>Nueva Venta - %s</h1>
                    </div>
                    <div style="padding:20px;">
                        <p>Se ha registrado una <strong>nueva venta</strong> en tu sede.</p>
                        <h3>Pedido #%d</h3>
                        <ul style="list-style:none;padding:0;">
                            <li><strong>Cliente:</strong> %s</li>
                            <li><strong>Email:</strong> %s</li>
                            <li><strong>Teléfono:</strong> %s</li>
                            <li><strong>Método de pago:</strong> %s</li>
                            <li><strong>Referencia:</strong> %s</li>
                        </ul>
                        <h3>Detalles:</h3>
                        <table style="width:100%%;border-collapse:collapse;">
                            <thead>
                                <tr style="background-color:#f2f2f2;">
                                    <th style="padding:8px;border:1px solid #ddd;text-align:left;">Producto</th>
                                    <th style="padding:8px;border:1px solid #ddd;text-align:center;">Cantidad</th>
                                    <th style="padding:8px;border:1px solid #ddd;text-align:right;">Precio</th>
                                </tr>
                            </thead>
                            <tbody>%s</tbody>
                        </table>
                        <hr style="margin:20px 0;">
                        <p style="text-align:right;font-size:18px;"><strong>Total: $%,.0f</strong></p>
                    </div>
                </body>
                </html>
                """.formatted(
                pedido.getSede().getNombre(),
                pedido.getId(),
                clienteNombre,
                clienteEmail,
                clienteTelefono,
                pedido.getMetodoPago() != null ? pedido.getMetodoPago() : "No especificado",
                pedido.getReferenciaPago() != null ? pedido.getReferenciaPago() : "N/A",
                detallesHtml,
                pedido.getTotal()
        );
    }

    private String construirHtmlCopiaMaestro(Pedido pedido) {
        String sedeNombre = pedido.getSede() != null ? pedido.getSede().getNombre() : "N/A";
        String clienteNombre = pedido.getCliente() != null ? pedido.getCliente().getNombre() : "N/A";

        return """
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                    <div style="background-color:#9C27B0;color:white;padding:20px;text-align:center;">
                        <h1>Copia de Venta - Pedido #%d</h1>
                    </div>
                    <div style="padding:20px;">
                        <p>Se ha registrado una nueva venta en el sistema.</p>
                        <ul style="list-style:none;padding:0;">
                            <li><strong>Pedido:</strong> #%d</li>
                            <li><strong>Sede:</strong> %s</li>
                            <li><strong>Cliente:</strong> %s</li>
                            <li><strong>Total:</strong> $%,.0f</li>
                            <li><strong>Estado:</strong> %s</li>
                            <li><strong>Referencia:</strong> %s</li>
                        </ul>
                    </div>
                </body>
                </html>
                """.formatted(
                pedido.getId(),
                pedido.getId(),
                sedeNombre,
                clienteNombre,
                pedido.getTotal(),
                pedido.getEstado() != null ? pedido.getEstado().name() : "N/A",
                pedido.getReferenciaPago() != null ? pedido.getReferenciaPago() : "N/A"
        );
    }
}
