package com.proyecto.servicios.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Genera y valida el JWT (momentaneo) que se le entrega al cliente al
 * iniciar sesion. Es un componente normal de Spring (no un AttributeConverter
 * de JPA), asi que si puede recibir @Value directamente.
 */
@Component
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-ms}")
    private long expiracionMs;

    public String generarToken(Integer clienteId, String correoElectronico) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expiracionMs);

        return Jwts.builder()
                .setSubject(String.valueOf(clienteId))
                .claim("correo", correoElectronico)
                .setIssuedAt(ahora)
                .setExpiration(expiracion)
                .signWith(obtenerLlaveFirma(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Integer obtenerClienteId(String token) {
        return Integer.valueOf(parsearClaims(token).getSubject());
    }

    private Claims parsearClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(obtenerLlaveFirma())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key obtenerLlaveFirma() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
