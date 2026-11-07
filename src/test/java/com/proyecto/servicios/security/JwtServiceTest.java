package com.proyecto.servicios.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String CLAVE = "clave-de-pruebas-suficientemente-larga-para-hs256-0123456789";

    private JwtService servicioConVigencia(long expiracionMs, String clave) {
        JwtService servicio = new JwtService();
        ReflectionTestUtils.setField(servicio, "secretKey", clave);
        ReflectionTestUtils.setField(servicio, "expiracionMs", expiracionMs);
        return servicio;
    }

    @Test
    void generarToken_tokenVigente_regresaElClienteId() {
        JwtService servicio = servicioConVigencia(60_000, CLAVE);

        String token = servicio.generarToken(25, "cliente@correo.com");

        assertThat(servicio.obtenerClienteId(token)).isEqualTo(25);
    }

    @Test
    void generarToken_incluyeFechaDeEmisionYExpiracionValidas() {
        JwtService servicio = servicioConVigencia(60_000, CLAVE);

        String token = servicio.generarToken(25, "cliente@correo.com");
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(CLAVE.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.getIssuedAt().toInstant()).isBeforeOrEqualTo(Instant.now());
        assertThat(claims.getExpiration().toInstant()).isAfter(Instant.now());
        assertThat(claims.get("correo", String.class)).isEqualTo("cliente@correo.com");
    }

    @Test
    void obtenerClienteId_tokenVencido_seRechaza() {
        JwtService servicio = servicioConVigencia(-60_000, CLAVE);

        String token = servicio.generarToken(25, "cliente@correo.com");

        assertThatThrownBy(() -> servicio.obtenerClienteId(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void obtenerClienteId_tokenFirmadoConOtraClave_seRechaza() {
        String token = servicioConVigencia(60_000, "otra-clave-distinta-tambien-larga-para-hs256-9876543210")
                .generarToken(25, "cliente@correo.com");
        JwtService servicio = servicioConVigencia(60_000, CLAVE);

        assertThatThrownBy(() -> servicio.obtenerClienteId(token)).isInstanceOf(SignatureException.class);
    }
}
