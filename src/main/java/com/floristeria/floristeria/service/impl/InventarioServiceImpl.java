package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.entity.Sede;
import com.floristeria.floristeria.entity.Producto;
import com.floristeria.floristeria.exception.AccesoDenegadoSedeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floristeria.floristeria.dto.InventarioResponseDTO;
import com.floristeria.floristeria.dto.InventarioUpdateRequestDTO;
import com.floristeria.floristeria.entity.Inventario;
import com.floristeria.floristeria.repository.InventarioRepository;
import com.floristeria.floristeria.service.InventarioService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InventarioServiceImpl implements InventarioService {

    private final InventarioRepository inventarioRepository;

    private static final String ROL_SUPERADMIN = "SUPERADMIN";
    private static final String MENSAJE_ACCESO_DENEGADO = "No tiene permisos para modificar el inventario de esta sede";
    private static final String MENSAJE_INVENTARIO_NO_ENCONTRADO = "Inventario no encontrado con id: ";

    @Override
    public InventarioResponseDTO actualizarInventarioLocal(Integer inventarioId, InventarioUpdateRequestDTO request,
            Integer usuarioSedeId, String rol) {

        Inventario inventario = inventarioRepository.findById(inventarioId)
                .orElseThrow(() -> new EntityNotFoundException(MENSAJE_INVENTARIO_NO_ENCONTRADO + inventarioId));

        if (!ROL_SUPERADMIN.equals(rol)) {
            Sede sede = inventario.getSede();
            if (sede == null) {
                throw new EntityNotFoundException("La sede asociada a este inventario ya no existe");
            }
            if (!sede.getId().equals(usuarioSedeId)) {
                throw new AccesoDenegadoSedeException(MENSAJE_ACCESO_DENEGADO);
            }
        }

        inventario.setPrecio(request.getPrecio());
        inventario.setStock(request.getStock());
        inventario.setDisponible(request.getDisponible());
        if (request.getDescuentoPorcentaje() != null) {
            inventario.setDescuentoPorcentaje(request.getDescuentoPorcentaje());
        }

        Inventario inventarioActualizado = inventarioRepository.save(inventario);

        Producto producto = inventarioActualizado.getProducto();
        Sede sede = inventarioActualizado.getSede();

        return InventarioResponseDTO.builder()
                .id(inventarioActualizado.getId())
                .productoNombre(producto != null ? producto.getNombre() : "Producto eliminado")
                .productoSku(producto != null ? producto.getSku() : "N/A")
                .sedeNombre(sede != null ? sede.getNombre() : "Sede eliminada")
                .precio(inventarioActualizado.getPrecio())
                .stock(inventarioActualizado.getStock())
                .disponible(inventarioActualizado.getDisponible())
                .descuentoPorcentaje(inventarioActualizado.getDescuentoPorcentaje())
                .precioFinal(calcularPrecioFinal(inventarioActualizado.getPrecio(), inventarioActualizado.getDescuentoPorcentaje()))
                .productoImagenUrl(producto != null ? producto.getImagenUrl() : null)
                .build();
    }

    @Override
    public List<InventarioResponseDTO> obtenerInventarioPorSede(Integer sedeId) {
        List<Inventario> inventarios = (sedeId == null)
                ? inventarioRepository.findAllActive()
                : inventarioRepository.findActiveBySedeId(sedeId);

        return inventarios.stream()
                .map(inventario -> {
                    Producto producto = inventario.getProducto();
                    Sede sede = inventario.getSede();

                    return InventarioResponseDTO.builder()
                            .id(inventario.getId())
                            .productoNombre(producto != null ? producto.getNombre() : "Producto eliminado")
                            .sedeNombre(sede != null ? sede.getNombre() : "Sede eliminada")
                            .precio(inventario.getPrecio())
                            .stock(inventario.getStock())
                            .disponible(inventario.getDisponible())
                            .descuentoPorcentaje(inventario.getDescuentoPorcentaje())
                            .precioFinal(calcularPrecioFinal(inventario.getPrecio(), inventario.getDescuentoPorcentaje()))
                            .productoImagenUrl(producto != null ? producto.getImagenUrl() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private BigDecimal calcularPrecioFinal(BigDecimal precio, Integer descuentoPorcentaje) {
        if (descuentoPorcentaje == null || descuentoPorcentaje <= 0) {
            return precio;
        }
        BigDecimal descuento = precio.multiply(BigDecimal.valueOf(descuentoPorcentaje))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return precio.subtract(descuento);
    }
}
