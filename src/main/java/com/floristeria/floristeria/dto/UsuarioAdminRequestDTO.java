package com.floristeria.floristeria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioAdminRequestDTO {

    public interface Create {}
    public interface Update {}

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    private String email;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(groups = Create.class, message = "La contraseña es obligatoria")
    @Size(groups = Create.class, min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "El rol es obligatorio")
    private String rol;

    private Integer sedeId;
}
