package com.floristeria.floristeria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioAdminRequestDTO {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String nombre;

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotBlank
    private String rol;

    private Integer sedeId;
}
