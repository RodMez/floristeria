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

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Async
    @Override
    public void notificarNuevaVenta(Pedido pedido) {
        try {
            String nombreSede = pedido.getSede() != null ? pedido.getSede().getNombre() : "Sede no disponible";

            // 1. Correo al Cliente (Recibo de compra)
            if (pedido.getCliente() != null && pedido.getCliente().getEmail() != null) {
                String asuntoCliente = "Recibo de tu compra - " + nombreSede;
                String htmlCliente = construirHtmlReciboCliente(pedido);
                enviarCorreoBrevo(pedido.getCliente().getEmail(), pedido.getCliente().getNombre(),
                        asuntoCliente, htmlCliente);
            }

            // 2. Correo a la Sede (Nueva venta)
            if (pedido.getSede() != null && pedido.getSede().getEmail() != null
                    && !pedido.getSede().getEmail().isBlank()) {
                String asuntoSede = "¡NUEVA VENTA! - " + nombreSede;
                String htmlSede = construirHtmlNuevaVenta(pedido);
                enviarCorreoBrevo(pedido.getSede().getEmail(), pedido.getSede().getNombre(),
                        asuntoSede, htmlSede);
            }

            // 3. Copia al Correo Maestro (si está habilitado)
            ConfiguracionTienda config = configuracionService.obtenerConfiguracion();
            if (Boolean.TRUE.equals(config.getEnviarCopiaMaestro())
                    && config.getCorreoMaestro() != null
                    && !config.getCorreoMaestro().isBlank()) {
                String asuntoMaestro = "¡NUEVA VENTA! - " + nombreSede;
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
        ConfiguracionTienda config = configuracionService.obtenerConfiguracion();
        String logoUrl = config.getLogoUrl() != null ? config.getLogoUrl() : "";
        String nombreSitio = config.getNombreSitio() != null ? config.getNombreSitio() : "TAO Boutique Floral";
        Integer sedeId = pedido.getSede() != null ? pedido.getSede().getId() : null;

        StringBuilder detallesHtml = new StringBuilder();
        StringBuilder resenasHtml = new StringBuilder();
        if (pedido.getDetalles() != null) {
            for (DetallePedido detalle : pedido.getDetalles()) {
                String nombreProducto = detalle.getProducto() != null
                        ? detalle.getProducto().getNombre() : "Producto eliminado";
                String nota = detalle.getNotaPersonalizacion() != null
                        ? detalle.getNotaPersonalizacion() : "N/A";

                String productUrl = (detalle.getProducto() != null && sedeId != null)
                        ? frontendUrl + "/tienda/sede/" + sedeId + "/producto/" + detalle.getProducto().getId()
                        : "#";
                String linkedNombre = String.format(
                        "<a href='%s' style='color:#E5BE6F;text-decoration:none;font-weight:bold;'>%s</a>",
                        productUrl, nombreProducto);

                detallesHtml.append(String.format(
                        "<tr><td style='padding:8px;border:1px solid #eee;'>%s</td>" +
                        "<td style='padding:8px;border:1px solid #eee;text-align:center;'>%d</td>" +
                        "<td style='padding:8px;border:1px solid #eee;text-align:right;'>$%,.0f</td>" +
                        "<td style='padding:8px;border:1px solid #eee;'>%s</td></tr>",
                        linkedNombre, detalle.getCantidad(), detalle.getPrecioUnitario(), nota));

                if (detalle.getProducto() != null && sedeId != null) {
                    String reviewUrl = productUrl;
                    resenasHtml.append(String.format(
                            "<a href='%s' style='display:inline-block;background-color:#E5BE6F;color:#3d3d3d;padding:10px 20px;border-radius:8px;text-decoration:none;font-weight:bold;font-size:14px;margin:4px;'>Reseñar: %s</a> ",
                            reviewUrl, nombreProducto));
                }
            }
        }

        String reviewSectionHtml = "";
        if (resenasHtml.length() > 0) {
            reviewSectionHtml = """
                    <div style="background-color:#FFF8E7;border:1px solid #E5BE6F30;border-radius:12px;padding:20px;margin:24px 0;text-align:center;">
                        <h3 style="color:#3d3d3d;margin:0 0 6px;">¿Qué te pareció tu compra?</h3>
                        <p style="color:#888;margin:0 0 14px;font-size:14px;">Tu opinión nos ayuda a mejorar</p>
                        %s
                    </div>
                    """.formatted(resenasHtml);
        }

        String fechaFormateada = pedido.getCreadoEn() != null
                ? pedido.getCreadoEn().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "Fecha no disponible";

        String direccionHtml;
        if (pedido.getDireccion() != null) {
            Direccion d = pedido.getDireccion();
            String detalles = d.getDetalles() != null ? d.getDetalles() : "Sin detalles adicionales";
            direccionHtml = """
                    <h3 style="color:#3d3d3d;margin:20px 0 8px;">Dirección de entrega:</h3>
                    <ul style="list-style:none;padding:0;color:#555;">
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

        String logoHtml = !logoUrl.isEmpty()
                ? "<img src='" + logoUrl + "' alt='Logo' width='80' style='border-radius:50%;margin-bottom:8px;' />"
                : "";

        return """
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background-color:#fafafa;">
                    <div style="background-color:#fafaf9;padding:28px 20px;text-align:center;border-bottom:3px solid #E5BE6F;">
                        %s
                        <h1 style="color:#3d3d3d;font-size:20px;margin:0;">%s</h1>
                        <p style="color:#999;font-size:13px;margin:4px 0 0;">Confirmación de Pedido</p>
                    </div>
                    <div style="padding:24px 20px;background-color:#ffffff;">
                        <p style="color:#555;">Hola <strong style="color:#3d3d3d;">%s</strong>,</p>
                        <p style="color:#555;">Tu pedido ha sido confirmado y el pago procesado exitosamente.</p>
                        <div style="background-color:#f9f9f9;border-radius:8px;padding:16px;margin:16px 0;">
                            <p style="margin:4px 0;color:#555;"><strong>Pedido:</strong> %s</p>
                            <p style="margin:4px 0;color:#555;"><strong>Referencia de pago:</strong> %s</p>
                            <p style="margin:4px 0;color:#555;"><strong>Fecha:</strong> %s</p>
                            <p style="margin:4px 0;color:#555;"><strong>Sede de despacho:</strong> %s</p>
                            <p style="margin:4px 0;color:#555;"><strong>Método de pago:</strong> %s</p>
                        </div>
                        %s
                        %s
                        <h3 style="color:#3d3d3d;margin:20px 0 8px;">Detalles del pedido:</h3>
                        <table style="width:100%%;border-collapse:collapse;">
                            <thead>
                                <tr style="background-color:#f5f5f5;">
                                    <th style="padding:8px;border:1px solid #eee;text-align:left;color:#555;">Producto</th>
                                    <th style="padding:8px;border:1px solid #eee;text-align:center;color:#555;">Cantidad</th>
                                    <th style="padding:8px;border:1px solid #eee;text-align:right;color:#555;">Precio</th>
                                    <th style="padding:8px;border:1px solid #eee;text-align:left;color:#555;">Notas</th>
                                </tr>
                            </thead>
                            <tbody>%s</tbody>
                        </table>
                        <hr style="margin:20px 0;border:none;border-top:1px solid #eee;">
                        <p style="text-align:right;color:#888;margin:4px 0;">Costo de Envío (Zona: %s): $%,.0f</p>
                        <p style="text-align:right;font-size:18px;color:#3d3d3d;margin:8px 0;"><strong>Total: $%,.0f</strong></p>
                        %s
                    </div>
                    <div style="background-color:#2c2c2c;padding:20px;text-align:center;border-radius:0 0 8px 8px;">
                        <p style="color:#999;font-size:13px;margin:0;">Gracias por tu compra</p>
                        <p style="color:#666;font-size:11px;margin:6px 0 0;">%s</p>
                    </div>
                </body>
                </html>
                """.formatted(
                logoHtml,
                nombreSitio,
                pedido.getCliente().getNombre(),
                pedido.getCodigo() != null ? pedido.getCodigo() : "N/A",
                pedido.getReferenciaPago() != null ? pedido.getReferenciaPago() : "N/A",
                fechaFormateada,
                pedido.getSede().getNombre(),
                pedido.getMetodoPago() != null ? pedido.getMetodoPago() : "No especificado",
                direccionHtml,
                notasEntregaHtml,
                detallesHtml,
                zonaEnvio, costoEnvio,
                pedido.getTotal(),
                reviewSectionHtml,
                nombreSitio
        );
    }

    private String construirHtmlNuevaVenta(Pedido pedido) {
        ConfiguracionTienda config = configuracionService.obtenerConfiguracion();
        String logoUrl = config.getLogoUrl() != null ? config.getLogoUrl() : "";
        String nombreSitio = config.getNombreSitio() != null ? config.getNombreSitio() : "TAO Boutique Floral";
        Integer sedeId = pedido.getSede() != null ? pedido.getSede().getId() : null;

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

                String productUrl = (detalle.getProducto() != null && sedeId != null)
                        ? frontendUrl + "/tienda/sede/" + sedeId + "/producto/" + detalle.getProducto().getId()
                        : "#";
                String linkedNombre = String.format(
                        "<a href='%s' style='color:#E5BE6F;text-decoration:none;font-weight:bold;'>%s</a>",
                        productUrl, nombreProducto);

                detallesHtml.append(String.format(
                        "<tr><td style='padding:8px;border:1px solid #eee;'>%s</td>" +
                        "<td style='padding:8px;border:1px solid #eee;text-align:center;'>%d</td>" +
                        "<td style='padding:8px;border:1px solid #eee;text-align:right;'>$%,.0f</td>" +
                        "<td style='padding:8px;border:1px solid #eee;'>%s</td></tr>",
                        linkedNombre, detalle.getCantidad(), detalle.getPrecioUnitario(), nota));
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
                    <h3 style="color:#3d3d3d;margin:20px 0 8px;">Dirección de entrega:</h3>
                    <ul style="list-style:none;padding:0;color:#555;">
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

        String logoHtml = !logoUrl.isEmpty()
                ? "<img src='" + logoUrl + "' alt='Logo' width='80' style='border-radius:50%;margin-bottom:8px;' />"
                : "";

        return """
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background-color:#fafafa;">
                    <div style="background-color:#fafaf9;padding:28px 20px;text-align:center;border-bottom:3px solid #E5BE6F;">
                        %s
                        <h1 style="color:#3d3d3d;font-size:20px;margin:0;">Nueva Venta</h1>
                        <p style="color:#999;font-size:13px;margin:4px 0 0;">%s</p>
                    </div>
                    <div style="padding:24px 20px;background-color:#ffffff;">
                        <p style="color:#555;">Se ha registrado una <strong style="color:#3d3d3d;">nueva venta</strong> en tu sede.</p>
                        <div style="background-color:#f9f9f9;border-radius:8px;padding:16px;margin:16px 0;">
                            <p style="margin:4px 0;color:#555;"><strong>Pedido:</strong> %s</p>
                            <p style="margin:4px 0;color:#555;"><strong>Referencia de pago:</strong> %s</p>
                            <p style="margin:4px 0;color:#555;"><strong>Fecha:</strong> %s</p>
                            <p style="margin:4px 0;color:#555;"><strong>Método de pago:</strong> %s</p>
                        </div>
                        <div style="background-color:#f9f9f9;border-radius:8px;padding:16px;margin:16px 0;">
                            <p style="margin:4px 0;color:#555;"><strong>Cliente:</strong> %s</p>
                            <p style="margin:4px 0;color:#555;"><strong>Email:</strong> %s</p>
                            <p style="margin:4px 0;color:#555;"><strong>Teléfono:</strong> %s</p>
                        </div>
                        %s
                        %s
                        <h3 style="color:#3d3d3d;margin:20px 0 8px;">Detalles:</h3>
                        <table style="width:100%%;border-collapse:collapse;">
                            <thead>
                                <tr style="background-color:#f5f5f5;">
                                    <th style="padding:8px;border:1px solid #eee;text-align:left;color:#555;">Producto</th>
                                    <th style="padding:8px;border:1px solid #eee;text-align:center;color:#555;">Cantidad</th>
                                    <th style="padding:8px;border:1px solid #eee;text-align:right;color:#555;">Precio</th>
                                    <th style="padding:8px;border:1px solid #eee;text-align:left;color:#555;">Notas</th>
                                </tr>
                            </thead>
                            <tbody>%s</tbody>
                        </table>
                        <hr style="margin:20px 0;border:none;border-top:1px solid #eee;">
                        <p style="text-align:right;color:#888;margin:4px 0;">Costo de Envío (Zona: %s): $%,.0f</p>
                        <p style="text-align:right;font-size:18px;color:#3d3d3d;margin:8px 0;"><strong>Total: $%,.0f</strong></p>
                    </div>
                    <div style="background-color:#2c2c2c;padding:20px;text-align:center;border-radius:0 0 8px 8px;">
                        <p style="color:#999;font-size:13px;margin:0;">Notificación de venta</p>
                        <p style="color:#666;font-size:11px;margin:6px 0 0;">%s</p>
                    </div>
                </body>
                </html>
                """.formatted(
                logoHtml,
                pedido.getSede().getNombre(),
                pedido.getCodigo() != null ? pedido.getCodigo() : "N/A",
                pedido.getReferenciaPago() != null ? pedido.getReferenciaPago() : "N/A",
                fechaFormateada,
                pedido.getMetodoPago() != null ? pedido.getMetodoPago() : "No especificado",
                clienteNombre,
                clienteEmail,
                clienteTelefono,
                direccionHtml,
                notasEntregaHtml,
                detallesHtml,
                zonaEnvio, costoEnvio,
                pedido.getTotal(),
                nombreSitio
        );
    }

    private String construirHtmlCopiaMaestro(Pedido pedido) {
        ConfiguracionTienda config = configuracionService.obtenerConfiguracion();
        String logoUrl = config.getLogoUrl() != null ? config.getLogoUrl() : "";
        String nombreSitio = config.getNombreSitio() != null ? config.getNombreSitio() : "TAO Boutique Floral";
        Integer sedeId = pedido.getSede() != null ? pedido.getSede().getId() : null;

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

                String productUrl = (detalle.getProducto() != null && sedeId != null)
                        ? frontendUrl + "/tienda/sede/" + sedeId + "/producto/" + detalle.getProducto().getId()
                        : "#";
                String linkedNombre = String.format(
                        "<a href='%s' style='color:#E5BE6F;text-decoration:none;font-weight:bold;'>%s</a>",
                        productUrl, nombreProducto);

                detallesHtml.append(String.format(
                        "<tr><td style='padding:8px;border:1px solid #eee;'>%s</td>" +
                        "<td style='padding:8px;border:1px solid #eee;text-align:center;'>%d</td>" +
                        "<td style='padding:8px;border:1px solid #eee;text-align:right;'>$%,.0f</td>" +
                        "<td style='padding:8px;border:1px solid #eee;'>%s</td></tr>",
                        linkedNombre, detalle.getCantidad(), detalle.getPrecioUnitario(), nota));
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
                    <h3 style="color:#3d3d3d;margin:20px 0 8px;">Dirección de entrega:</h3>
                    <ul style="list-style:none;padding:0;color:#555;">
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

        String nombreSede = pedido.getSede() != null ? pedido.getSede().getNombre() : "Sede no disponible";

        String logoHtml = !logoUrl.isEmpty()
                ? "<img src='" + logoUrl + "' alt='Logo' width='80' style='border-radius:50%;margin-bottom:8px;' />"
                : "";

        return """
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background-color:#fafafa;">
                    <div style="background-color:#fafaf9;padding:28px 20px;text-align:center;border-bottom:3px solid #E5BE6F;">
                        %s
                        <h1 style="color:#3d3d3d;font-size:20px;margin:0;">Nueva Venta</h1>
                        <p style="color:#999;font-size:13px;margin:4px 0 0;">Sede: %s</p>
                    </div>
                    <div style="padding:24px 20px;background-color:#ffffff;">
                        <p style="color:#555;">Se ha registrado una <strong style="color:#3d3d3d;">nueva venta</strong> en el sistema.</p>
                        <div style="background-color:#f9f9f9;border-radius:8px;padding:16px;margin:16px 0;">
                            <p style="margin:4px 0;color:#555;"><strong>Pedido:</strong> %s</p>
                            <p style="margin:4px 0;color:#555;"><strong>Referencia de pago:</strong> %s</p>
                            <p style="margin:4px 0;color:#555;"><strong>Fecha:</strong> %s</p>
                            <p style="margin:4px 0;color:#555;"><strong>Método de pago:</strong> %s</p>
                        </div>
                        <div style="background-color:#f9f9f9;border-radius:8px;padding:16px;margin:16px 0;">
                            <p style="margin:4px 0;color:#555;"><strong>Cliente:</strong> %s</p>
                            <p style="margin:4px 0;color:#555;"><strong>Email:</strong> %s</p>
                            <p style="margin:4px 0;color:#555;"><strong>Teléfono:</strong> %s</p>
                        </div>
                        %s
                        %s
                        <h3 style="color:#3d3d3d;margin:20px 0 8px;">Detalles:</h3>
                        <table style="width:100%%;border-collapse:collapse;">
                            <thead>
                                <tr style="background-color:#f5f5f5;">
                                    <th style="padding:8px;border:1px solid #eee;text-align:left;color:#555;">Producto</th>
                                    <th style="padding:8px;border:1px solid #eee;text-align:center;color:#555;">Cantidad</th>
                                    <th style="padding:8px;border:1px solid #eee;text-align:right;color:#555;">Precio</th>
                                    <th style="padding:8px;border:1px solid #eee;text-align:left;color:#555;">Notas</th>
                                </tr>
                            </thead>
                            <tbody>%s</tbody>
                        </table>
                        <hr style="margin:20px 0;border:none;border-top:1px solid #eee;">
                        <p style="text-align:right;color:#888;margin:4px 0;">Costo de Envío (Zona: %s): $%,.0f</p>
                        <p style="text-align:right;font-size:18px;color:#3d3d3d;margin:8px 0;"><strong>Total: $%,.0f</strong></p>
                    </div>
                    <div style="background-color:#2c2c2c;padding:20px;text-align:center;border-radius:0 0 8px 8px;">
                        <p style="color:#999;font-size:13px;margin:0;">Notificación de venta</p>
                        <p style="color:#666;font-size:11px;margin:6px 0 0;">%s</p>
                    </div>
                </body>
                </html>
                """.formatted(
                logoHtml,
                nombreSede,
                pedido.getCodigo() != null ? pedido.getCodigo() : "N/A",
                pedido.getReferenciaPago() != null ? pedido.getReferenciaPago() : "N/A",
                fechaFormateada,
                pedido.getMetodoPago() != null ? pedido.getMetodoPago() : "No especificado",
                clienteNombre,
                clienteEmail,
                clienteTelefono,
                direccionHtml,
                notasEntregaHtml,
                detallesHtml,
                zonaEnvio, costoEnvio,
                pedido.getTotal(),
                nombreSitio
        );
    }
}
