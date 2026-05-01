package com.floristeria.floristeria.service;

import com.floristeria.floristeria.dto.UsuarioAdminRequestDTO;
import com.floristeria.floristeria.dto.UsuarioAdminResponseDTO;

import java.util.List;

public interface UsuarioAdminService {

    List<UsuarioAdminResponseDTO> listarTodos();

    UsuarioAdminResponseDTO crearUsuario(UsuarioAdminRequestDTO request);

    UsuarioAdminResponseDTO actualizarUsuario(Integer id, UsuarioAdminRequestDTO request);

    void eliminarUsuario(Integer id);
}
