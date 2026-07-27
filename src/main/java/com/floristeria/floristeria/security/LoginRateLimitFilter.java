package com.floristeria.floristeria.security;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS_PER_MINUTE = 5;
    private static final int MAX_CONSECUTIVE_FAILURES = 10;
    private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000;

    private final Map<String, RateLimitEntry> ipAttempts = new ConcurrentHashMap<>();
    private final Map<String, LockoutEntry> ipLockouts = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return !"POST".equals(method)
                || (!"/api/auth/login".equals(path) && !"/api/v1/clientes/auth/login".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);

        // 1. Lockout check primero (bloqueo más severo)
        LockoutEntry lockout = ipLockouts.get(clientIp);
        if (lockout != null && lockout.failures >= MAX_CONSECUTIVE_FAILURES) {
            long elapsed = System.currentTimeMillis() - lockout.lockedAt;
            if (elapsed < LOCKOUT_DURATION_MS) {
                long remainingMin = ((LOCKOUT_DURATION_MS - elapsed) / 60_000) + 1;
                send429(response,
                        "IP bloqueada temporalmente por demasiados intentos fallidos. Intenta de nuevo en "
                                + remainingMin + " minuto(s).");
                return;
            }
        }

        // 2. Rate limit check
        RateLimitEntry entry = ipAttempts.compute(clientIp, (key, existing) -> {
            if (existing == null || System.currentTimeMillis() - existing.windowStart > 60_000) {
                return new RateLimitEntry(System.currentTimeMillis(), new AtomicInteger(1));
            }
            existing.attempts.incrementAndGet();
            return existing;
        });

        if (entry.attempts.get() > MAX_ATTEMPTS_PER_MINUTE) {
            send429(response, "Demasiados intentos de inicio de sesión. Intenta de nuevo en 1 minuto.");
            ipLockouts.compute(clientIp, (key, existing) -> {
                if (existing == null) return null;
                existing.failures++;
                if (existing.failures >= MAX_CONSECUTIVE_FAILURES) {
                    existing.lockedAt = System.currentTimeMillis();
                }
                return existing;
            });
            return;
        }

        // 3. Ejecutar request
        filterChain.doFilter(request, response);

        // 4. Actualizar lockout según resultado de auth
        int status = response.getStatus();
        if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
            ipLockouts.compute(clientIp, (key, existing) -> {
                if (existing == null) {
                    return new LockoutEntry(1, 0);
                }
                existing.failures++;
                if (existing.failures >= MAX_CONSECUTIVE_FAILURES) {
                    existing.lockedAt = System.currentTimeMillis();
                }
                return existing;
            });
        } else if (status == HttpStatus.OK.value()) {
            ipLockouts.remove(clientIp);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void send429(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\",\"mensaje\":\"" + message + "\"}");
    }

    private static class RateLimitEntry {
        final long windowStart;
        final AtomicInteger attempts;

        RateLimitEntry(long windowStart, AtomicInteger attempts) {
            this.windowStart = windowStart;
            this.attempts = attempts;
        }
    }

    private static class LockoutEntry {
        int failures;
        long lockedAt;

        LockoutEntry(int failures, long lockedAt) {
            this.failures = failures;
            this.lockedAt = lockedAt;
        }
    }
}
