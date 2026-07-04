package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.SedeRequestDTO;
import com.floristeria.floristeria.dto.SedeResponseDTO;
import com.floristeria.floristeria.entity.EstadoPedido;
import com.floristeria.floristeria.entity.Inventario;
import com.floristeria.floristeria.entity.Producto;
import com.floristeria.floristeria.entity.Sede;
import com.floristeria.floristeria.repository.InventarioRepository;
import com.floristeria.floristeria.repository.PedidoRepository;
import com.floristeria.floristeria.repository.ProductoRepository;
import com.floristeria.floristeria.repository.SedeRepository;
import com.floristeria.floristeria.repository.UsuarioAdminRepository;
import com.floristeria.floristeria.repository.ZonaDomicilioRepository;
import com.floristeria.floristeria.service.SedeService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SedeServiceImpl implements SedeService {

    private final SedeRepository sedeRepository;
    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;
    private final UsuarioAdminRepository usuarioAdminRepository;
    private final PedidoRepository pedidoRepository;
    private final ZonaDomicilioRepository zonaDomicilioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SedeResponseDTO> listarTodas() {
        return sedeRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SedeResponseDTO crearSede(SedeRequestDTO requestDTO) {
        // Validar que no exista otra sede con la misma ciudad
        String ciudad = requestDTO.getCiudad().trim();
        if (sedeRepository.existsByCiudadIgnoreCaseUnaccent(ciudad)) {
            throw new IllegalArgumentException("Ya existe una sede en la ciudad: " + ciudad);
        }

        // 1. Crear y guardar la nueva sede
        Sede sede = new Sede();
        sede.setNombre(requestDTO.getNombre().trim());
        sede.setCiudad(ciudad);
        sede.setWhatsapp(requestDTO.getTelefonoWhatsapp());
        sede.setInstagramUrl(requestDTO.getInstagramUrl());
        sede.setFacebookUrl(requestDTO.getFacebookUrl());
        sede.setTiktokUrl(requestDTO.getTiktokUrl());
        sede.setEmail(requestDTO.getEmail());

        Sede sedeGuardada = sedeRepository.save(sede);

        // 2. Sincronizar inventario: crear registros para todos los productos existentes
        List<Producto> todosProductos = productoRepository.findAll();

        List<Inventario> inventarios = todosProductos.stream()
                .map(producto -> Inventario.builder()
                        .sede(sedeGuardada)
                        .producto(producto)
                        .stock(0)
                        .precio(java.math.BigDecimal.ZERO)
                        .disponible(Boolean.FALSE)
                        .build())
                .collect(Collectors.toList());

        inventarioRepository.saveAll(inventarios);

        return toResponseDTO(sedeGuardada);
    }

    @Override
    public SedeResponseDTO actualizarSede(Integer id, SedeRequestDTO requestDTO) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada con id: " + id));

        // Validar que no exista otra sede con la misma ciudad (excluyendo la actual)
        String ciudad = requestDTO.getCiudad().trim();
        if (sedeRepository.existsByCiudadIgnoreCaseUnaccentAndIdNot(ciudad, id)) {
            throw new IllegalArgumentException("Ya existe otra sede en la ciudad: " + ciudad);
        }

        sede.setNombre(requestDTO.getNombre().trim());
        sede.setCiudad(ciudad);
        sede.setWhatsapp(requestDTO.getTelefonoWhatsapp());
        sede.setInstagramUrl(requestDTO.getInstagramUrl());
        sede.setFacebookUrl(requestDTO.getFacebookUrl());
        sede.setTiktokUrl(requestDTO.getTiktokUrl());
        sede.setEmail(requestDTO.getEmail());

        Sede sedeActualizada = sedeRepository.save(sede);
        return toResponseDTO(sedeActualizada);
    }

    @Override
    public void eliminarSede(Integer id) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada con id: " + id));

        log.info("Intentando eliminar sede id={}, ciudad={}", id, sede.getCiudad());

        List<Inventario> bloqueantes = inventarioRepository.findBySede_IdAndDisponibleTrueAndStockGreaterThan(id, 0);
        log.info("Productos disponibles (disp=true, stock>0): {}", bloqueantes.size());
        for (Inventario inv : bloqueantes) {
            log.info("  - Producto id={}, nombre={}, stock={}, disponible={}",
                    inv.getProducto() != null ? inv.getProducto().getId() : "N/A",
                    inv.getProducto() != null ? inv.getProducto().getNombre() : "Producto eliminado",
                    inv.getStock(), inv.getDisponible());
        }

        if (!bloqueantes.isEmpty()) {
            throw new IllegalStateException(
                    "No se puede eliminar la sede porque tiene productos disponibles. Desactiva o cambia el stock de los productos primero.");
        }

        List<EstadoPedido> estadosFinales = List.of(EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO);
        boolean hayPedidosActivos = pedidoRepository.existsBySede_IdAndEstadoNotIn(id, estadosFinales);
        log.info("Pedidos activos (no ENTREGADO/CANCELADO): {}", hayPedidosActivos);

        if (hayPedidosActivos) {
            throw new IllegalStateException(
                    "No se puede eliminar la sede porque tiene pedidos activos. Espera a que se completen o cancelen todos los pedidos.");
        }

        LocalDateTime now = LocalDateTime.now();
        inventarioRepository.softDeleteBySedeId(id, now);
        usuarioAdminRepository.softDeleteBySedeId(id, now);
        zonaDomicilioRepository.softDeleteBySedeId(id, now);

        sede.setDeletedAt(now);
        sedeRepository.save(sede);
    }

    private SedeResponseDTO toResponseDTO(Sede sede) {
        return SedeResponseDTO.builder()
                .id(sede.getId())
                .nombre(sede.getNombre())
                .ciudad(sede.getCiudad())
                .telefonoWhatsapp(sede.getWhatsapp())
                .instagramUrl(sede.getInstagramUrl())
                .facebookUrl(sede.getFacebookUrl())
                .tiktokUrl(sede.getTiktokUrl())
                .email(sede.getEmail())
                .build();
    }
}
