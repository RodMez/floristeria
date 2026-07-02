package com.floristeria.floristeria.service;

import com.floristeria.floristeria.dto.ClienteActualizarRequestDTO;
import com.floristeria.floristeria.dto.ClienteAuthResponseDTO;
import com.floristeria.floristeria.dto.ClienteLoginDTO;
import com.floristeria.floristeria.dto.ClientePasswordRequestDTO;
import com.floristeria.floristeria.dto.ClientePerfilResponseDTO;
import com.floristeria.floristeria.dto.ClienteRegistroDTO;

public interface ClienteAuthService {

    ClienteAuthResponseDTO registrar(ClienteRegistroDTO request);

    ClienteAuthResponseDTO login(ClienteLoginDTO request);

    ClientePerfilResponseDTO actualizarPerfil(Integer clienteId, ClienteActualizarRequestDTO request);

    void cambiarPassword(Integer clienteId, ClientePasswordRequestDTO request);
}