package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.entity.ConfiguracionTienda;
import com.floristeria.floristeria.entity.DetallePedido;
import com.floristeria.floristeria.entity.Direccion;
import com.floristeria.floristeria.entity.Pedido;
import com.floristeria.floristeria.entity.Producto;
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
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
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

    // ── Brand colors ──
    private static final String C_MUSTARD = "#E5BE6F";
    private static final String C_MUSTARD_DARK = "#8B7230";
    private static final String C_SAGE = "#7A8A73";
    private static final String C_ROSE = "#EAC3BD";
    private static final String C_BG = "#FAFAF9";
    private static final String C_WHITE = "#FFFFFF";
    private static final String C_TEXT = "#3D3D3D";
    private static final String C_TEXT_SEC = "#78716C";
    private static final String C_TEXT_MUTED = "#A8A29E";
    private static final String C_BORDER = "#E7E5E4";

    @Async
    @Override
    public void notificarNuevaVenta(Pedido pedido) {
        try {
            String nombreSede = pedido.getSede() != null ? pedido.getSede().getNombre() : "Sede no disponible";

            if (pedido.getCliente() != null && pedido.getCliente().getEmail() != null) {
                String asuntoCliente = "Recibo de tu compra - " + nombreSede;
                String htmlCliente = construirHtmlReciboCliente(pedido);
                enviarCorreoBrevo(pedido.getCliente().getEmail(), pedido.getCliente().getNombre(),
                        asuntoCliente, htmlCliente);
            }

            if (pedido.getSede() != null && pedido.getSede().getEmail() != null
                    && !pedido.getSede().getEmail().isBlank()) {
                String asuntoSede = "\uD83C\uDF3A NUEVA VENTA - " + nombreSede;
                String htmlSede = construirHtmlNuevaVenta(pedido);
                enviarCorreoBrevo(pedido.getSede().getEmail(), pedido.getSede().getNombre(),
                        asuntoSede, htmlSede);
            }

            ConfiguracionTienda config = configuracionService.obtenerConfiguracion();
            if (Boolean.TRUE.equals(config.getEnviarCopiaMaestro())
                    && config.getCorreoMaestro() != null
                    && !config.getCorreoMaestro().isBlank()) {
                String asuntoMaestro = "\uD83C\uDF3A NUEVA VENTA - " + nombreSede;
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

        Map<String, Object> sender = Map.of("email", brevoSenderEmail, "name", brevoSenderName);
        Map<String, String> to = Map.of("email", toEmail, "name", toName != null ? toName : "");

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
            log.error("Brevo respondió con estado {} para {}", response.getStatusCode(), toEmail);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Google Fonts link for Cinzel
    // ═══════════════════════════════════════════════════════════════

    private String fontLink() {
        return "<link href='https://fonts.googleapis.com/css2?family=Cinzel:wght@400;500;600;700&display=swap' rel='stylesheet'>";
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Inline brand styles
    // ═══════════════════════════════════════════════════════════════

    private String bodyStyle() {
        return "font-family:'Cinzel',Georgia,'Times New Roman',serif;"
                + "background-color:" + C_BG + ";"
                + "margin:0;padding:0;-webkit-text-size-adjust:none;";
    }

    private String cardStyle() {
        return "background-color:" + C_WHITE + ";border:1px solid " + C_BORDER
                + ";border-radius:8px;padding:16px;margin:0 0 16px 0;";
    }

    private String cardTitleStyle() {
        return "color:" + C_MUSTARD_DARK + ";font-size:11px;font-weight:600;text-transform:uppercase;"
                + "letter-spacing:1px;margin:0 0 12px 0;font-family:'Cinzel',Georgia,'Times New Roman',serif;";
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Status Badge
    // ═══════════════════════════════════════════════════════════════

    private String buildStatusBadge(String estado) {
        if (estado == null) return "";
        String label;
        String bg;
        String textColor;
        switch (estado) {
            case "PENDIENTE_PAGO": label = "Pendiente de pago"; bg = C_ROSE; textColor = "#3D3D3D"; break;
            case "PAGADO":         label = "Pagado";           bg = "#FCD34D"; textColor = "#92400E"; break;
            case "EN_PREPARACION": label = "En preparación";   bg = "#BFDBFE"; textColor = "#1E40AF"; break;
            case "EN_CAMINO":      label = "En camino";        bg = "#E9D5FF"; textColor = "#6B21A8"; break;
            case "ENTREGADO":      label = "Entregado";         bg = C_SAGE;   textColor = "#FFFFFF"; break;
            case "CANCELADO":      label = "Cancelado";         bg = "#FCA5A5"; textColor = "#991B1B"; break;
            default:              label = estado;               bg = "#E7E5E4"; textColor = "#44403C"; break;
        }
        return "<span style='display:inline-block;background-color:" + bg + ";color:" + textColor
                + ";padding:4px 12px;border-radius:20px;font-size:12px;font-weight:600;"
                + "font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>" + label + "</span>";
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Section wrapper
    // ═══════════════════════════════════════════════════════════════

    private String wrapCard(String content) {
        return "<table width='100%' cellpadding='0' cellspacing='0' style='margin:0 0 16px 0;'>"
                + "<tr><td style='" + cardStyle() + "'>"
                + content
                + "</td></tr></table>";
    }

    private String sectionTitle(String title) {
        return "<p style='" + cardTitleStyle() + "'>" + title + "</p>";
    }

    private String infoRow(String label, String value) {
        return "<tr><td style='padding:3px 0;color:" + C_TEXT_SEC + ";font-size:13px;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;width:120px;vertical-align:top;'>"
                + label + "</td>"
                + "<td style='padding:3px 0;color:" + C_TEXT + ";font-size:13px;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>"
                + value + "</td></tr>";
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Product row
    // ═══════════════════════════════════════════════════════════════

    private String buildProductRow(DetallePedido detalle, Integer sedeId) {
        Producto producto = detalle.getProducto();
        String nombre = producto != null ? producto.getNombre() : "Producto eliminado";
        String sku = producto != null && producto.getSku() != null ? producto.getSku() : "";
        String imgUrl = producto != null && producto.getImagenUrl() != null ? producto.getImagenUrl() : "";
        String nota = detalle.getNotaPersonalizacion() != null && !detalle.getNotaPersonalizacion().isBlank()
                ? detalle.getNotaPersonalizacion() : null;

        String productUrl = (producto != null && sedeId != null)
                ? frontendUrl + "/tienda/sede/" + sedeId + "/producto/" + producto.getId()
                : "#";

        String imgHtml;
        if (!imgUrl.isEmpty()) {
            imgHtml = "<img src='" + imgUrl + "' width='40' height='40' style='border-radius:6px;display:block;object-fit:cover;width:40px;height:40px;' alt='' />";
        } else {
            imgHtml = "<div style='width:40px;height:40px;border-radius:6px;background-color:" + C_MUSTARD + "20;'></div>";
        }

        String notaHtml = "";
        if (nota != null) {
            notaHtml = "<p style='margin:2px 0 0 0;font-size:11px;color:" + C_ROSE + ";font-style:italic;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>"
                    + "\uD83D\uDCDD " + nota + "</p>";
        }

        String cantidad = String.valueOf(detalle.getCantidad());
        String precio = "$" + formatCurrency(detalle.getPrecioUnitario());
        String subtotal = "$" + formatCurrency(BigDecimal.valueOf(detalle.getCantidad()).multiply(detalle.getPrecioUnitario()));

        return "<tr>"
                + "<td style='padding:8px 8px 8px 0;border-bottom:1px solid " + C_BORDER + ";vertical-align:top;width:48px;'>"
                + imgHtml
                + "</td>"
                + "<td style='padding:8px 8px;border-bottom:1px solid " + C_BORDER + ";vertical-align:top;'>"
                + "<a href='" + productUrl + "' style='color:" + C_MUSTARD + ";text-decoration:none;font-weight:600;font-size:13px;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>" + nombre + "</a>"
                + (sku.isEmpty() ? "" : "<span style='display:block;color:" + C_TEXT_MUTED + ";font-size:11px;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>" + sku + "</span>")
                + notaHtml
                + "</td>"
                + "<td style='padding:8px 8px;border-bottom:1px solid " + C_BORDER + ";text-align:center;vertical-align:middle;color:" + C_TEXT_SEC + ";font-size:13px;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>" + cantidad + "</td>"
                + "<td style='padding:8px 8px;border-bottom:1px solid " + C_BORDER + ";text-align:right;vertical-align:middle;color:" + C_TEXT + ";font-size:13px;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>" + precio + "</td>"
                + "<td style='padding:8px 0 8px 8px;border-bottom:1px solid " + C_BORDER + ";text-align:right;vertical-align:middle;color:" + C_MUSTARD_DARK + ";font-weight:700;font-size:13px;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>" + subtotal + "</td>"
                + "</tr>";
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Product table
    // ═══════════════════════════════════════════════════════════════

    private String buildProductTable(List<DetallePedido> detalles, Integer sedeId) {
        StringBuilder rows = new StringBuilder();
        if (detalles != null) {
            for (DetallePedido d : detalles) {
                rows.append(buildProductRow(d, sedeId));
            }
        }

        return "<table width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse;'>"
                + "<thead>"
                + "<tr style='background-color:" + C_MUSTARD + ";'>"
                + "<th style='padding:8px 8px 8px 0;text-align:left;color:" + C_WHITE + ";font-size:11px;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;width:48px;'></th>"
                + "<th style='padding:8px 8px;text-align:left;color:" + C_WHITE + ";font-size:11px;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>Producto</th>"
                + "<th style='padding:8px 8px;text-align:center;color:" + C_WHITE + ";font-size:11px;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>Cant</th>"
                + "<th style='padding:8px 8px;text-align:right;color:" + C_WHITE + ";font-size:11px;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>Precio</th>"
                + "<th style='padding:8px 0 8px 8px;text-align:right;color:" + C_WHITE + ";font-size:11px;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>Subtotal</th>"
                + "</tr>"
                + "</thead>"
                + "<tbody>" + rows.toString() + "</tbody>"
                + "</table>";
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Header (unified for all templates)
    // ═══════════════════════════════════════════════════════════════

    private String buildHeader(String logoUrl, String nombreSitio, String title, String subtitle) {
        String logoHtml = !logoUrl.isEmpty()
                ? "<img src='" + logoUrl + "' alt='Logo' width='60' height='60' style='border-radius:50%;display:block;margin:0 auto 12px auto;width:60px;height:60px;' />"
                : "";

        return "<table width='100%' cellpadding='0' cellspacing='0' style='margin:0 0 24px 0;'>"
                + "<tr><td style='text-align:center;padding:32px 20px 20px 20px;background-color:" + C_WHITE + ";border-bottom:3px solid " + C_MUSTARD + ";border-radius:8px 8px 0 0;'>"
                + logoHtml
                + "<h1 style='color:" + C_TEXT + ";font-size:18px;margin:0;font-weight:700;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;letter-spacing:0.5px;'>" + nombreSitio + "</h1>"
                + "<p style='color:" + C_TEXT_SEC + ";font-size:13px;margin:4px 0 0 0;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>" + subtitle + "</p>"
                + (title.isEmpty() ? "" : "<h2 style='color:" + C_MUSTARD_DARK + ";font-size:16px;margin:16px 0 0 0;font-weight:600;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>" + title + "</h2>")
                + "</td></tr></table>";
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Footer (unified — NO black background)
    // ═══════════════════════════════════════════════════════════════

    private String buildFooter(String nombreSitio, String whatsappUrl) {
        String whatsappHtml = "";
        if (whatsappUrl != null && !whatsappUrl.isBlank()) {
            whatsappHtml = "<p style='margin:12px 0 0 0;'>"
                    + "<a href='" + whatsappUrl + "' style='display:inline-block;background-color:" + C_WHITE + ";color:" + C_MUSTARD + ";border:2px solid " + C_MUSTARD + ";padding:10px 24px;border-radius:50px;text-decoration:none;font-size:13px;font-weight:600;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>"
                    + "\uD83D\uDCAC Escr\u00EDbenos por WhatsApp</a></p>";
        }

        return "<table width='100%' cellpadding='0' cellspacing='0' style='margin:24px 0 0 0;'>"
                + "<tr><td style='padding:0 20px;'>"
                + "<hr style='border:none;border-top:2px solid " + C_MUSTARD + ";margin:0 0 20px 0;' />"
                + "</td></tr>"
                + "<tr><td style='text-align:center;padding:0 20px 32px 20px;background-color:" + C_WHITE + ";border-radius:0 0 8px 8px;'>"
                + "<p style='color:" + C_TEXT_SEC + ";font-size:13px;margin:0 0 4px 0;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>Gracias por tu compra</p>"
                + "<p style='color:" + C_MUSTARD_DARK + ";font-size:12px;margin:0 0 12px 0;font-weight:700;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>" + nombreSitio + "</p>"
                + whatsappHtml
                + "<p style='color:" + C_TEXT_MUTED + ";font-size:10px;margin:16px 0 0 0;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>"
                + "Este es un correo autom\u00E1tico. No respondas a este mensaje.</p>"
                + "</td></tr></table>";
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Estimated delivery date
    // ═══════════════════════════════════════════════════════════════

    private String calcularFechaEntregaEstimada(Pedido pedido) {
        if (pedido.getCreadoEn() == null) return "";
        LocalDate base = pedido.getCreadoEn().toLocalDate();
        LocalDate estimada = base.plusDays(1);
        if (estimada.getDayOfWeek() == DayOfWeek.SATURDAY) {
            estimada = estimada.plusDays(2);
        } else if (estimada.getDayOfWeek() == DayOfWeek.SUNDAY) {
            estimada = estimada.plusDays(1);
        }
        return estimada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Format currency
    // ═══════════════════════════════════════════════════════════════

    private String formatCurrency(BigDecimal value) {
        if (value == null) return "0";
        long rounded = value.setScale(0, RoundingMode.HALF_UP).longValue();
        return java.lang.String.format(java.util.Locale.US, "%,d", rounded);
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Order info card
    // ═══════════════════════════════════════════════════════════════

    private String buildOrderInfoCard(Pedido pedido, String fechaFormateada, String estado, boolean showEstado) {
        String entregaEstimada = calcularFechaEntregaEstimada(pedido);
        String badge = showEstado ? "<p style='margin:12px 0 0 0;'>" + buildStatusBadge(estado) + "</p>" : "";

        StringBuilder html = new StringBuilder();
        html.append(sectionTitle("Datos del Pedido"));
        html.append("<table cellpadding='0' cellspacing='0'>");
        html.append(infoRow("Pedido", pedido.getCodigo() != null ? pedido.getCodigo() : "N/A"));
        html.append(infoRow("Referencia", pedido.getReferenciaPago() != null ? pedido.getReferenciaPago() : "N/A"));
        html.append(infoRow("Fecha", fechaFormateada));
        html.append(infoRow("Sede", pedido.getSede() != null ? pedido.getSede().getNombre() : "N/A"));
        html.append(infoRow("M\u00E9todo", pedido.getMetodoPago() != null ? pedido.getMetodoPago() : "N/A"));
        if (!entregaEstimada.isEmpty()) {
            html.append(infoRow("Entrega estimada", entregaEstimada));
        }
        html.append("</table>");
        html.append(badge);

        return wrapCard(html.toString());
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Delivery card
    // ═══════════════════════════════════════════════════════════════

    private String buildDeliveryCard(Pedido pedido) {
        StringBuilder html = new StringBuilder();
        html.append(sectionTitle("Direcci\u00F3n de Entrega"));

        if (pedido.getDireccion() != null) {
            Direccion d = pedido.getDireccion();
            html.append("<table cellpadding='0' cellspacing='0'>");
            html.append(infoRow("Alias", d.getAlias() != null ? d.getAlias() : "—"));
            html.append(infoRow("Direcci\u00F3n", d.getDireccion() != null ? d.getDireccion() : "—"));
            html.append(infoRow("Ciudad", d.getCiudad() != null ? d.getCiudad() : "—"));
            if (d.getDetalles() != null && !d.getDetalles().isBlank()) {
                html.append(infoRow("Detalles", d.getDetalles()));
            }
            html.append("</table>");
        } else {
            html.append("<p style='color:" + C_TEXT_SEC + ";font-size:13px;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>Direcci\u00F3n no disponible</p>");
        }

        String zonaEnvio = "N/A";
        if (pedido.getDireccion() != null && pedido.getDireccion().getZonaDomicilio() != null) {
            ZonaDomicilio zona = pedido.getDireccion().getZonaDomicilio();
            zonaEnvio = zona.getLocalidad()
                    + (zona.getBarrio() != null ? " - " + zona.getBarrio() : "");
        }
        BigDecimal costoEnvio = pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : BigDecimal.ZERO;

        html.append("<hr style='border:none;border-top:1px solid " + C_BORDER + ";margin:10px 0;' />");
        html.append("<table cellpadding='0' cellspacing='0'>");
        html.append(infoRow("Zona de env\u00EDo", zonaEnvio));
        html.append(infoRow("Costo de env\u00EDo", "$" + formatCurrency(costoEnvio)));
        html.append("</table>");

        if (pedido.getNotasEntrega() != null && !pedido.getNotasEntrega().isBlank()) {
            html.append("<p style='color:" + C_ROSE + ";font-size:12px;margin:8px 0 0 0;font-style:italic;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>"
                    + "\uD83D\uDCDD Notas de entrega: " + pedido.getNotasEntrega() + "</p>");
        }

        return wrapCard(html.toString());
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Totals section
    // ═══════════════════════════════════════════════════════════════

    private String buildTotalsSection(Pedido pedido, String zonaEnvio) {
        BigDecimal costoEnvio = pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : BigDecimal.ZERO;
        String totalStr = "$" + formatCurrency(pedido.getTotal());

        String envioHtml = "";
        if (costoEnvio.compareTo(BigDecimal.ZERO) > 0) {
            envioHtml = "<tr><td style='padding:3px 0;color:" + C_TEXT_SEC + ";font-size:13px;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>Env\u00EDo" + zonaEnvio + "</td>"
                    + "<td style='padding:3px 0;text-align:right;color:" + C_TEXT_SEC + ";font-size:13px;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>$" + formatCurrency(costoEnvio) + "</td></tr>";
        }

        return "<table width='100%' cellpadding='0' cellspacing='0' style='margin:16px 0 0 0;'>"
                + "<tr>"
                + "<td style='padding:12px 16px;background-color:" + C_MUSTARD + "15;border-radius:8px;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0'>"
                + envioHtml
                + "<tr><td style='padding:3px 0;color:" + C_MUSTARD_DARK + ";font-size:16px;font-weight:700;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>Total</td>"
                + "<td style='padding:3px 0;text-align:right;color:" + C_MUSTARD_DARK + ";font-size:16px;font-weight:700;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>" + totalStr + "</td></tr>"
                + "</table>"
                + "</td>"
                + "</tr></table>";
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Review section (client only)
    // ═══════════════════════════════════════════════════════════════

    private String buildReviewSection(List<DetallePedido> detalles, Integer sedeId) {
        StringBuilder btns = new StringBuilder();
        if (detalles != null && sedeId != null) {
            for (DetallePedido d : detalles) {
                if (d.getProducto() != null) {
                    String url = frontendUrl + "/tienda/sede/" + sedeId + "/producto/" + d.getProducto().getId();
                    btns.append("<a href='" + url + "' style='display:inline-block;background-color:" + C_MUSTARD + ";color:" + C_TEXT + ";padding:8px 18px;border-radius:50px;text-decoration:none;font-size:12px;font-weight:600;margin:4px 4px 0 0;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>"
                            + "\u2B50 " + d.getProducto().getNombre() + "</a> ");
                }
            }
        }

        if (btns.length() == 0) return "";

        return "<table width='100%' cellpadding='0' cellspacing='0' style='margin:0 0 16px 0;'>"
                + "<tr><td style='background-color:" + C_WHITE + ";border:1px solid " + C_MUSTARD + "40;border-radius:12px;padding:24px 16px;text-align:center;'>"
                + "<p style='color:" + C_TEXT + ";font-size:15px;margin:0 0 4px 0;font-weight:600;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>\u00BFQu\u00E9 te pareci\u00F3 tu compra?</p>"
                + "<p style='color:" + C_TEXT_SEC + ";font-size:12px;margin:0 0 14px 0;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>Tu opini\u00F3n nos ayuda a mejorar</p>"
                + "<div style='margin:0 -4px;'>" + btns.toString() + "</div>"
                + "</td></tr></table>";
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Client info card (admin/sede/maestro)
    // ═══════════════════════════════════════════════════════════════

    private String buildClientInfoCard(Pedido pedido) {
        String nombre = pedido.getCliente() != null ? pedido.getCliente().getNombre() : "N/A";
        String email = pedido.getCliente() != null ? pedido.getCliente().getEmail() : "N/A";
        String telefono = pedido.getCliente() != null ? pedido.getCliente().getTelefono() : "N/A";

        StringBuilder html = new StringBuilder();
        html.append(sectionTitle("Datos del Cliente"));
        html.append("<table cellpadding='0' cellspacing='0'>");
        html.append(infoRow("Nombre", nombre));
        html.append(infoRow("Email", "<a href='mailto:" + email + "' style='color:" + C_MUSTARD + ";text-decoration:none;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>" + email + "</a>"));
        html.append(infoRow("Tel\u00E9fono", "<a href='tel:" + telefono + "' style='color:" + C_MUSTARD + ";text-decoration:none;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>" + telefono + "</a>"));
        html.append("</table>");

        return wrapCard(html.toString());
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Admin link button
    // ═══════════════════════════════════════════════════════════════

    private String buildAdminLink(Pedido pedido) {
        String url = frontendUrl + "/admin/pedidos";
        return "<p style='text-align:center;margin:16px 0 0 0;'>"
                + "<a href='" + url + "' style='display:inline-block;background-color:" + C_MUSTARD + ";color:" + C_TEXT + ";padding:12px 28px;border-radius:50px;text-decoration:none;font-size:14px;font-weight:700;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>"
                + "Ver en administraci\u00F3n</a></p>";
    }

    // ═══════════════════════════════════════════════════════════════
    //  TEMPLATE 1: Cliente
    // ═══════════════════════════════════════════════════════════════

    private String construirHtmlReciboCliente(Pedido pedido) {
        ConfiguracionTienda config = configuracionService.obtenerConfiguracion();
        String logoUrl = config.getLogoUrl() != null ? config.getLogoUrl() : "";
        String nombreSitio = config.getNombreSitio() != null ? config.getNombreSitio() : "TAO Boutique Floral";
        String whatsappUrl = config.getWhatsappGeneral();
        Integer sedeId = pedido.getSede() != null ? pedido.getSede().getId() : null;
        String estado = pedido.getEstado() != null ? pedido.getEstado().name() : "";

        String fechaFormateada = pedido.getCreadoEn() != null
                ? pedido.getCreadoEn().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "Fecha no disponible";

        String zonaEnvio = " N/A";
        if (pedido.getDireccion() != null && pedido.getDireccion().getZonaDomicilio() != null) {
            ZonaDomicilio zona = pedido.getDireccion().getZonaDomicilio();
            String zonaStr = zona.getLocalidad() + (zona.getBarrio() != null ? " " + zona.getBarrio() : "");
            zonaEnvio = " (" + zonaStr + ")";
        }

        String saludo = pedido.getCliente() != null
                ? "Hola <strong style='color:" + C_TEXT + ";'>" + pedido.getCliente().getNombre() + "</strong>"
                : "Hola";

        String header = buildHeader(logoUrl, nombreSitio, "\u00A1Tu pedido est\u00E1 confirmado!", nombreSitio);
        String orderInfo = buildOrderInfoCard(pedido, fechaFormateada, estado, true);
        String delivery = buildDeliveryCard(pedido);
        String products = wrapCard(sectionTitle("Productos") + buildProductTable(pedido.getDetalles(), sedeId));
        String totals = buildTotalsSection(pedido, zonaEnvio);
        String review = buildReviewSection(pedido.getDetalles(), sedeId);
        String footer = buildFooter(nombreSitio, whatsappUrl);

        return htmlWrap(header, saludo, orderInfo, delivery, products, totals, review, footer);
    }

    // ═══════════════════════════════════════════════════════════════
    //  TEMPLATE 2: Sede
    // ═══════════════════════════════════════════════════════════════

    private String construirHtmlNuevaVenta(Pedido pedido) {
        ConfiguracionTienda config = configuracionService.obtenerConfiguracion();
        String logoUrl = config.getLogoUrl() != null ? config.getLogoUrl() : "";
        String nombreSitio = config.getNombreSitio() != null ? config.getNombreSitio() : "TAO Boutique Floral";
        String nombreSede = pedido.getSede() != null ? pedido.getSede().getNombre() : "Sede no disponible";
        Integer sedeId = pedido.getSede() != null ? pedido.getSede().getId() : null;
        String estado = pedido.getEstado() != null ? pedido.getEstado().name() : "";

        String fechaFormateada = pedido.getCreadoEn() != null
                ? pedido.getCreadoEn().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "Fecha no disponible";

        String zonaEnvio = "";
        if (pedido.getDireccion() != null && pedido.getDireccion().getZonaDomicilio() != null) {
            ZonaDomicilio zona = pedido.getDireccion().getZonaDomicilio();
            String zonaStr = zona.getLocalidad() + (zona.getBarrio() != null ? " " + zona.getBarrio() : "");
            zonaEnvio = " (" + zonaStr + ")";
        }

        String header = buildHeader(logoUrl, nombreSitio, "", nombreSede);

        String alerta = wrapCard(
                "<p style='text-align:center;color:" + C_SAGE + ";font-size:18px;font-weight:700;margin:0 0 4px 0;"
                + "font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>\u2705 \u00A1Nueva venta!</p>"
                + "<p style='text-align:center;color:" + C_TEXT_SEC + ";font-size:13px;margin:0;"
                + "font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>Se ha registrado una venta en tu sede. Revisa los detalles a continuaci\u00F3n.</p>"
                + "<p style='text-align:center;margin:12px 0 0 0;'>" + buildStatusBadge(estado) + "</p>"
        );

        String orderInfo = buildOrderInfoCard(pedido, fechaFormateada, estado, false);
        String clientInfo = buildClientInfoCard(pedido);
        String delivery = buildDeliveryCard(pedido);
        String products = wrapCard(sectionTitle("Productos") + buildProductTable(pedido.getDetalles(), sedeId));
        String totals = buildTotalsSection(pedido, zonaEnvio);
        String adminLink = buildAdminLink(pedido);
        String footer = buildFooter(nombreSitio, null);

        return htmlWrap(header, alerta, orderInfo, clientInfo, delivery, products, totals, adminLink, footer);
    }

    // ═══════════════════════════════════════════════════════════════
    //  TEMPLATE 3: Maestro
    // ═══════════════════════════════════════════════════════════════

    private String construirHtmlCopiaMaestro(Pedido pedido) {
        ConfiguracionTienda config = configuracionService.obtenerConfiguracion();
        String logoUrl = config.getLogoUrl() != null ? config.getLogoUrl() : "";
        String nombreSitio = config.getNombreSitio() != null ? config.getNombreSitio() : "TAO Boutique Floral";
        String nombreSede = pedido.getSede() != null ? pedido.getSede().getNombre() : "Sede no disponible";
        Integer sedeId = pedido.getSede() != null ? pedido.getSede().getId() : null;
        String estado = pedido.getEstado() != null ? pedido.getEstado().name() : "";

        String fechaFormateada = pedido.getCreadoEn() != null
                ? pedido.getCreadoEn().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "Fecha no disponible";

        String zonaEnvio = "";
        if (pedido.getDireccion() != null && pedido.getDireccion().getZonaDomicilio() != null) {
            ZonaDomicilio zona = pedido.getDireccion().getZonaDomicilio();
            String zonaStr = zona.getLocalidad() + (zona.getBarrio() != null ? " " + zona.getBarrio() : "");
            zonaEnvio = " (" + zonaStr + ")";
        }

        String header = buildHeader(logoUrl, nombreSitio, "", nombreSede + " — Nueva venta");

        String resumen = wrapCard(
                "<table width='100%' cellpadding='0' cellspacing='0'>"
                + "<tr>"
                + "<td style='text-align:center;padding:8px;'>"
                + "<p style='color:" + C_TEXT_SEC + ";font-size:11px;margin:0 0 2px 0;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>Total</p>"
                + "<p style='color:" + C_MUSTARD_DARK + ";font-size:20px;font-weight:700;margin:0;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>$" + formatCurrency(pedido.getTotal()) + "</p>"
                + "</td>"
                + "<td style='text-align:center;padding:8px;'>"
                + "<p style='color:" + C_TEXT_SEC + ";font-size:11px;margin:0 0 2px 0;font-family:\"Cinzel\",Georgia,\"Times New Roman\",serif;'>Estado</p>"
                + "<p style='margin:0;'>" + buildStatusBadge(estado) + "</p>"
                + "</td>"
                + "</tr>"
                + "</table>"
        );

        String orderInfo = buildOrderInfoCard(pedido, fechaFormateada, estado, false);
        String clientInfo = buildClientInfoCard(pedido);
        String delivery = buildDeliveryCard(pedido);
        String products = wrapCard(sectionTitle("Productos") + buildProductTable(pedido.getDetalles(), sedeId));
        String totals = buildTotalsSection(pedido, zonaEnvio);
        String adminLink = buildAdminLink(pedido);
        String footer = buildFooter(nombreSitio, null);

        return htmlWrap(header, resumen, orderInfo, clientInfo, delivery, products, totals, adminLink, footer);
    }

    // ═══════════════════════════════════════════════════════════════
    //  WRAPPER: Full HTML document
    // ═══════════════════════════════════════════════════════════════

    private String htmlWrap(String... sections) {
        StringBuilder body = new StringBuilder();
        for (String s : sections) {
            body.append(s);
        }

        return "<!DOCTYPE html>"
                + "<html lang='es'>"
                + "<head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'>"
                + fontLink()
                + "</head>"
                + "<body style='" + bodyStyle() + "'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' role='article' aria-label='Correo TAO Boutique Floral'>"
                + "<tr><td align='center' style='padding:20px 10px;'>"
                + "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;background-color:" + C_WHITE + ";border-radius:8px;overflow:hidden;'>"
                + "<tr><td style='padding:0;'>"
                + body.toString()
                + "</td></tr>"
                + "</table>"
                + "</td></tr>"
                + "</table>"
                + "</body></html>";
    }
}
