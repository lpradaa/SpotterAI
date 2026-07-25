package com.spotterai.backend.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    // La clave se inyecta desde la variable de entorno JWT_SECRET.
    // Nunca debe vivir en el código: quien la tenga puede firmar tokens de cualquier usuario.
    // Para HS256 necesitamos al menos 32 caracteres (256 bits).
    private final SecretKey signingKey;

    // Tiempo de validez del token: 24 horas (en milisegundos)
    private final long EXPIRATION_TIME = 86400000;

    public JwtUtil(@Value("${spotterai.jwt.secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET debe existir y tener al menos 32 caracteres. " +
                "Genera uno con: openssl rand -base64 48");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Genera la clave encriptada que usará el algoritmo
    private SecretKey getSigningKey() {
        return signingKey;
    }

    /**
     * MÉTODO 1: FABRICAR EL TOKEN
     * Se llama cuando el usuario hace Login correctamente.
     */
    public String generarToken(String email) {
        return Jwts.builder()
                .subject(email) // El "dueño" del token será el email
                .issuedAt(new Date()) // Fecha de creación
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // Fecha de caducidad
                .signWith(getSigningKey()) // Lo firmamos criptográficamente
                .compact();
    }

    /**
     * MÉTODO 2: EXTRAER EL EMAIL
     * Sirve para saber quién está haciendo la petición leyendo su token.
     */
    public String extraerEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * MÉTODO 3: VALIDAR EL TOKEN
     * Comprueba que el token no ha sido manipulado por un hacker y no ha caducado.
     */
    public boolean validarToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false; // Si falla la firma o está caducado, devuelve false
        }
    }
}