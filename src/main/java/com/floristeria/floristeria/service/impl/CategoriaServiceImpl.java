package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.CategoriaRequestDTO;
import com.floristeria.floristeria.dto.CategoriaResponseDTO;
import com.floristeria.floristeria.entity.Categoria;
import com.floristeria.floristeria.repository.CategoriaRepository;
import com.floristeria.floristeria.service.CategoriaService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listarTodas() {
        return categoriaRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CategoriaResponseDTO crearCategoria(CategoriaRequestDTO requestDTO) {
        Categoria categoria = new Categoria();
        categoria.setNombre(requestDTO.getNombre());

        Categoria categoriaGuardada = categoriaRepository.save(categoria);
        return toResponseDTO(categoriaGuardada);
    }

    @Override
    public CategoriaResponseDTO actualizarCategoria(Integer id, CategoriaRequestDTO requestDTO) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con id: " + id));

        categoria.setNombre(requestDTO.getNombre());

        Categoria categoriaActualizada = categoriaRepository.save(categoria);
        return toResponseDTO(categoriaActualizada);
    }

    @Override
    public void eliminarCategoria(Integer id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con id: " + id));
        categoria.setDeletedAt(LocalDateTime.now());
        categoriaRepository.save(categoria);
    }

    private CategoriaResponseDTO toResponseDTO(Categoria categoria) {
        return CategoriaResponseDTO.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .build();
    }
}
