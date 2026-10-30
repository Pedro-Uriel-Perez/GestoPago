package com.proyecto.servicios.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GestoPagoAesCipherTest {

    // Llave/IV aleatorias de prueba, no las de GestoPago.
    private static final String SECRET_KEY = "533JchFhKkgsa1l4t4tOc+PrZm/d6wf/SDueBGZhCOA=";
    private static final String IV = "Lt552TSKgLBaBGUX6M7T3w==";

    private final GestoPagoAesCipher cipher = new GestoPagoAesCipher(SECRET_KEY, IV);

    @Test
    void encriptarYDesencriptar_conElPayloadDeEjemploDeLaEspecificacion_regresaElMismoTexto() {
        String payload = "{\"idServicio\":79, \"idProducto\":209 , \"referencia\": \"40425475190118187271\", "
                + "\"montoPago\": 9999, \"telefono\":\"1111111111\", \"horaLocal\":\"20200401222821\"}";

        String cifrado = cipher.encriptar(payload);
        String descifrado = cipher.desencriptar(cifrado);

        assertThat(cifrado).isNotEqualTo(payload);
        assertThat(descifrado).isEqualTo(payload);
    }

    @Test
    void encriptar_conElMismoTextoDosVeces_daElMismoResultado() {
        String payload = "{\"idProducto\":582,\"idServicio\":133}";

        String primero = cipher.encriptar(payload);
        String segundo = cipher.encriptar(payload);

        assertThat(primero).isEqualTo(segundo);
    }
}
