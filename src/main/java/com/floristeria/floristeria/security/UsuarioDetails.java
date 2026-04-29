package com.floristeria.floristeria.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import lombok.Getter;

@Getter
public class UsuarioDetails extends User {

    private final Integer sedeId;
    private final String rol;

    public UsuarioDetails(String username, String password, Collection<? extends GrantedAuthority> authorities,
            Integer sedeId, String rol) {
        super(username, password, authorities);
        this.sedeId = sedeId;
        this.rol = rol;
    }
}
