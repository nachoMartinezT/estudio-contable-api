package com.guidapixel.contable.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // Clave secreta de 256 bits (32 caracteres) para firmar.
    // EN PRODUCCIÓN: Usar @Value("${jwt.secret}")
    private static final String SECRET_KEY = "Est0EsUnaClav3MuyS3gur4ParaGu1daC0ntabl3Api2026!!!";

    public String generateToken(UserDetails userDetails, Long tenantId) {
        Map<String, Object> extraClaims = new HashMap<>();
        // ¡Magia! Guardamos el tenantId DENTRO del token.
        // Así, en cada petición futura, sabremos a qué estudio pertenece sin ir a la DB.
        extraClaims.put("tenantId", tenantId);

        return buildToken(extraClaims, userDetails);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername()) // El email es el subject
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 horas
                .signWith(getSignInKey()) // Firmamos con el algoritmo nuevo de JJWT 0.12+
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Método para sacar el tenantId del token
    public Long extractTenantId(String token) {
        return extractClaim(token, claims -> claims.get("tenantId", Long.class));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(SECRET_KEY.getBytes()));
        return Keys.hmacShaKeyFor(keyBytes);
    }
}