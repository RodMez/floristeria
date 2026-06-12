package com.floristeria.floristeria.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            final String email = jwtService.extractUsername(token);
            final Claims claims = jwtService.extractAllClaims(token);
            final String rol = claims.get("rol", String.class);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                if ("CLIENTE".equals(rol)) {
                    // === FLUJO CLIENTE ===
                    Integer clienteId = claims.get("clienteId", Integer.class);

                    ClienteDetails clienteDetails = new ClienteDetails(
                            email,
                            "", // sin contraseña en el contexto
                            Collections.singletonList(new SimpleGrantedAuthority("CLIENTE")),
                            clienteId
                    );

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            clienteDetails,
                            null,
                            clienteDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    // === FLUJO ADMIN ===
                    UserDetails dbUserDetails = userDetailsService.loadUserByUsername(email);
                    boolean isTokenValid = jwtService.isTokenValid(token, dbUserDetails);

                    if (isTokenValid) {
                        Integer sedeId = claims.get("sedeId", Integer.class);

                        UsuarioDetails usuarioDetails = new UsuarioDetails(
                                email,
                                dbUserDetails.getPassword(),
                                Collections.singletonList(new SimpleGrantedAuthority(rol)),
                                sedeId,
                                rol
                        );

                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                usuarioDetails,
                                null,
                                usuarioDetails.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception e) {
            // Si el token es inválido/expirado, no establecemos autenticación
            // pero dejamos que la petición continúe — Spring Security decidirá
            // si la ruta es pública (200) o protegida (403)
            logger.warn("Error procesando token JWT: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
