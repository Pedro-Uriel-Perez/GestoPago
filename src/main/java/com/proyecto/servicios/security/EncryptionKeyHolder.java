package com.proyecto.servicios.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Los AttributeConverter de JPA los instancia Hibernate directamente (no
 * Spring), por lo que no se les puede inyectar @Value normalmente. Este
 * componente si es manejado por Spring, recibe la clave secreta desde
 * application.properties, y la expone de forma estatica para que
 * AesEncryptionUtil (usado por los converters) pueda leerla.
 */
@Component
public class EncryptionKeyHolder {

    private static volatile String claveSecreta;

    @Value("${security.aes.secret-key}")
    public void inicializar(String clave) {
        EncryptionKeyHolder.claveSecreta = clave;
    }

    public static String getClaveSecreta() {
        if (claveSecreta == null) {
            throw new IllegalStateException(
                    "La clave de cifrado (security.aes.secret-key) aun no ha sido inicializada por Spring");
        }
        return claveSecreta;
    }
}
