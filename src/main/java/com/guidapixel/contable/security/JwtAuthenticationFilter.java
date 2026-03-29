package com.guidapixel.contable.security;

import com.guidapixel.contable.multitenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService; // Spring Security interface

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Si no hay token, dejamos pasar (quizás va a /login)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // Quitar "Bearer "

        try {
            userEmail = jwtService.extractUsername(jwt);
            Long tenantId = jwtService.extractTenantId(jwt); // Extraemos el ID del estudio

            // 2. Si hay usuario y no está autenticado aún en el contexto
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // 3. Autenticar en Spring Security
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // 4. CRÍTICO: Configurar el TenantContext
                    if (tenantId != null) {
                        TenantContext.setTenantId(tenantId);
                    }
                }
            }
        } catch (Exception e) {
            // Si el token está mal formado o expirado, simplemente no autenticamos.
            // Spring Security rechazará la petición más adelante.
            System.err.println("Error procesando JWT: " + e.getMessage());
        }

        // 5. Continuar con la cadena de filtros (Controller, etc.)
        filterChain.doFilter(request, response);

        // 6. Limpieza (Importante para no contaminar hilos en pool)
        TenantContext.clear();
    }
}