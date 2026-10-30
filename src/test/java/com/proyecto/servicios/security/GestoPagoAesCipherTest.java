package com.proyecto.servicios.security;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class GestoPagoAesCipherTest {

    private final GestoPagoAesCipher cipher = new GestoPagoAesCipher(aleatorioBase64(32), aleatorioBase64(16));

    /** Llave/IV generadas en cada corrida: ningun valor fijo queda en el repo. */
    static String aleatorioBase64(int bytes) {
        byte[] valor = new byte[bytes];
        new SecureRandom().nextBytes(valor);
        return Base64.getEncoder().encodeToString(valor);
    }

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
