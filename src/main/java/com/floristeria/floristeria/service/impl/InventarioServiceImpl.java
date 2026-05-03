package com.floristeria.floristeria.service.impl;

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
            Integer inventarioSedeId = inventario.getSede().getId();
            if (!inventarioSedeId.equals(usuarioSedeId)) {
                throw new AccesoDenegadoSedeException(MENSAJE_ACCESO_DENEGADO);
            }
        }

        inventario.setPrecio(request.getPrecio());
        inventario.setStock(request.getStock());
        inventario.setDisponible(request.getDisponible());

        Inventario inventarioActualizado = inventarioRepository.save(inventario);

        return InventarioResponseDTO.builder()
                .id(inventarioActualizado.getId())
                .productoNombre(inventarioActualizado.getProducto().getNombre())
                .sedeNombre(inventarioActualizado.getSede().getNombre())
                .precio(inventarioActualizado.getPrecio())
                .stock(inventarioActualizado.getStock())
                .disponible(inventarioActualizado.getDisponible())
                .build();
    }

    @Override
    public List<InventarioResponseDTO> obtenerInventarioPorSede(Integer sedeId) {
        return inventarioRepository.findBySede_Id(sedeId).stream()
                .map(inventario -> InventarioResponseDTO.builder()
                        .id(inventario.getId())
                        .productoNombre(inventario.getProducto().getNombre())
                        .sedeNombre(inventario.getSede().getNombre())
                        .precio(inventario.getPrecio())
                        .stock(inventario.getStock())
                        .disponible(inventario.getDisponible())
                        .build())
                .collect(Collectors.toList());
    }
}
