package com.floristeria.floristeria.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floristeria.floristeria.dto.ProductoCatalogoDTO;
import com.floristeria.floristeria.dto.ProductoCatalogoDetalleDTO;
import com.floristeria.floristeria.entity.Categoria;
import com.floristeria.floristeria.entity.Inventario;
import com.floristeria.floristeria.entity.Producto;
import com.floristeria.floristeria.entity.Sede;
import com.floristeria.floristeria.repository.InventarioRepository;
import com.floristeria.floristeria.repository.ReseñaRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final InventarioRepository inventarioRepository;
    private final ReseñaRepository reseñaRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public List<ProductoCatalogoDTO> obtenerCatalogoPorSede(Integer sedeId) {
        return inventarioRepository.findBySede_IdAndDisponibleTrueAndStockGreaterThan(sedeId, 0).stream()
                .map(inv -> {
                    Producto producto = inv.getProducto();
                    List<String> categoriasNombres = producto.getCategorias().stream()
                            .map(Categoria::getNombre)
                            .collect(Collectors.toList());

                    BigDecimal precioConDescuento = calcularPrecioConDescuento(inv.getPrecio(), inv.getDescuentoPorcentaje());
                    Integer prodId = producto.getId();

                    return ProductoCatalogoDTO.builder()
                            .productoId(prodId)
                            .nombre(producto.getNombre())
                            .descripcion(producto.getDescripcion())
                            .imagenUrl(producto.getImagenUrl())
                            .sku(producto.getSku())
                            .categoriasNombres(categoriasNombres)
                            .precio(inv.getPrecio())
                            .descuentoPorcentaje(inv.getDescuentoPorcentaje())
                            .precioConDescuento(precioConDescuento)
                            .stock(inv.getStock())
                            .disponible(true)
                            .ratingAverage(reseñaRepository.findAverageRatingByProductoId(prodId))
                            .ratingCount(reseñaRepository.findCountByProductoId(prodId))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductoCatalogoDetalleDTO obtenerDetalleProductoPorSede(Integer sedeId, Integer productoId) {
        Inventario inv = inventarioRepository.findByProducto_IdAndSede_Id(productoId, sedeId);

        if (inv == null || inv.getStock() == null || inv.getStock() <= 0 || !inv.getDisponible()) {
            throw new EntityNotFoundException(
                    "Producto no disponible en esta sede o sin stock");
        }

        Producto p = inv.getProducto();
        Sede s = inv.getSede();
        BigDecimal precioFinal = calcularPrecioConDescuento(inv.getPrecio(), inv.getDescuentoPorcentaje());

        List<String> categoriasNombres = p.getCategorias() != null
                ? p.getCategorias().stream().map(Categoria::getNombre).toList()
                : Collections.emptyList();

        Integer prodId = p.getId();

        return ProductoCatalogoDetalleDTO.builder()
                .inventarioId(inv.getId())
                .productoId(prodId)
                .nombre(p.getNombre())
                .descripcion(p.getDescripcion())
                .imagenUrl(p.getImagenUrl())
                .sku(p.getSku())
                .sedeId(s.getId())
                .sedeNombre(s.getNombre())
                .precioBase(inv.getPrecio())
                .descuentoPorcentaje(inv.getDescuentoPorcentaje())
                .precioFinal(precioFinal)
                .stock(inv.getStock())
                .disponible(inv.getDisponible())
                .categoriasNombres(categoriasNombres)
                .ratingAverage(reseñaRepository.findAverageRatingByProductoId(prodId))
                .ratingCount(reseñaRepository.findCountByProductoId(prodId))
                .build();
    }

    @Transactional(readOnly = true)
    public String generarMetaFeedXml() {
        List<Inventario> inventarios = inventarioRepository.findAvailableForFeed();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss xmlns:g=\"http://base.google.com/ns/1.0\" version=\"2.0\">\n");
        xml.append("  <channel>\n");
        xml.append("    <title>Catálogo Floristería</title>\n");
        xml.append("    <link>").append(escapeXml(frontendUrl)).append("</link>\n");
        xml.append("    <description>Catálogo de productos para Meta Business</description>\n");

        for (Inventario inv : inventarios) {
            Producto p = inv.getProducto();
            Sede s = inv.getSede();

            String sku = p.getSku() != null ? p.getSku() : String.valueOf(p.getId());
            String id = sku + "-" + s.getId();
            BigDecimal precioFinal = calcularPrecioConDescuento(inv.getPrecio(), inv.getDescuentoPorcentaje());

            xml.append("    <item>\n");
            xml.append("      <g:id>").append(escapeXml(id)).append("</g:id>\n");
            xml.append("      <g:title>").append(escapeXml(p.getNombre())).append(" - ").append(escapeXml(s.getNombre())).append("</g:title>\n");
            xml.append("      <g:description>").append(escapeXml(p.getDescripcion())).append("</g:description>\n");
            xml.append("      <g:link>").append(escapeXml(frontendUrl)).append("/tienda/sede/").append(s.getId()).append("/producto/").append(p.getId()).append("</g:link>\n");
            xml.append("      <g:image_link>").append(escapeXml(p.getImagenUrl())).append("</g:image_link>\n");
            xml.append("      <g:brand>Floristería</g:brand>\n");
            xml.append("      <g:condition>new</g:condition>\n");
            xml.append("      <g:availability>in stock</g:availability>\n");
            xml.append("      <g:price>").append(precioFinal).append(" COP</g:price>\n");
            xml.append("    </item>\n");
        }

        xml.append("  </channel>\n");
        xml.append("</rss>");

        return xml.toString();
    }

    private BigDecimal calcularPrecioConDescuento(BigDecimal precio, Integer descuentoPorcentaje) {
        if (descuentoPorcentaje != null && descuentoPorcentaje > 0) {
            BigDecimal descuentoAmount = precio
                    .multiply(new BigDecimal(descuentoPorcentaje))
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
            return precio.subtract(descuentoAmount);
        }
        return precio;
    }

    private static String escapeXml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
