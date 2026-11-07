package com.proyecto.servicios.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    private static CorsConfiguration configuracion(String origenes) {
        return new CorsConfig(origenes).corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/clientes/1"));
    }

    @Test
    void separaLosOrigenesPorComaEIgnoraLosVacios() {
        CorsConfiguration cors = configuracion(" http://localhost:* , ,https://*.onrender.com ");

        assertThat(cors.getAllowedOriginPatterns()).containsExactly("http://localhost:*", "https://*.onrender.com");
    }

    @Test
    void aceptaOrigenesQueCoincidenConElPatron() {
        CorsConfiguration cors = configuracion("http://localhost:*,https://*.onrender.com");

        assertThat(cors.checkOrigin("http://localhost:5000")).isEqualTo("http://localhost:5000");
        assertThat(cors.checkOrigin("https://gestopago-web.onrender.com")).isEqualTo("https://gestopago-web.onrender.com");
        assertThat(cors.checkOrigin("https://sitio-ajeno.com")).isNull();
    }

    @Test
    void permiteElHeaderAuthorizationPeroNoCookies() {
        CorsConfiguration cors = configuracion("http://localhost:*");

        assertThat(cors.getAllowedHeaders()).contains("Authorization", "Content-Type");
        assertThat(cors.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(cors.getAllowCredentials()).isFalse();
    }
}
