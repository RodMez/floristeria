package com.floristeria.floristeria.security; 

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpMethod;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;
    private final WebhookRateLimitFilter webhookRateLimitFilter;
    private final CustomUserDetailsService customUserDetailsService;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // 1. Rutas Públicas (no requieren autenticación)
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/auth/**", "/api/v1/clientes/auth/**").permitAll()
                        // Endpoints públicos de catálogo
                        .requestMatchers("/api/v1/catalogo/**", "/api/v1/sedes/**", "/api/v1/categorias/**", "/api/v1/zonas-domicilio/**", "/api/v1/configuracion", "/api/v1/banners/**", "/api/v1/resenas/producto/**").permitAll()
                        .requestMatchers("/api/v1/webhooks/**").permitAll()
                        // 2. Creación de pedidos manuales: solo ADMIN/SUPERADMIN (precios se validan contra inventario server-side)
                        .requestMatchers(HttpMethod.POST, "/api/v1/pedidos").hasAnyAuthority("ADMIN", "SUPERADMIN")
                        // 3. Rutas de Clientes (requieren rol CLIENTE autenticado)
                        .requestMatchers("/api/v1/clientes/**").hasAuthority("CLIENTE")
                        // 4. Reseñas: crear y consultar estado solo CLIENTE (lectura pública ya cubierta arriba)
                        .requestMatchers(HttpMethod.POST, "/api/v1/resenas").hasAuthority("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/resenas/producto/*/estado").hasAuthority("CLIENTE")
                        // 5. Rutas exclusivas del Superadmin
                        .requestMatchers("/api/superadmin/**").hasAuthority("SUPERADMIN")
                        // 6. Banners y Reseñas admin: solo SUPERADMIN (recursos globales, no por sede)
                        .requestMatchers("/api/admin/banners/**").hasAuthority("SUPERADMIN")
                        .requestMatchers("/api/admin/resenas/**").hasAuthority("SUPERADMIN")
                        // 7. Rutas de los Administradores de Sede
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ADMIN", "SUPERADMIN")
                        // 5. Cualquier otra ruta requiere autenticación
                        .anyRequest().authenticated()
                )
                .addFilterBefore(webhookRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // USAMOS OriginPatterns PARA SOPORTAR EL ASTERISCO (*) DE VERCEL
        configuration.setAllowedOriginPatterns(allowedOrigins);
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}