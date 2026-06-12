package com.floristeria.floristeria.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import lombok.Getter;

@Getter
public class ClienteDetails extends User {

    private final Integer clienteId;

    public ClienteDetails(String username, String password, Collection<? extends GrantedAuthority> authorities,
            Integer clienteId) {
        super(username, password, authorities);
        this.clienteId = clienteId;
    }
}