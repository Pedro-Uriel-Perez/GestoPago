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

    /**
     * Revisado (Sonar java:S2696): escribir un campo estatico desde un
     * metodo de instancia es justo el proposito de esta clase. Spring solo
     * inyecta @Value en metodos de instancia, y los AttributeConverter que
     * necesitan la clave los crea Hibernate, sin acceso al contexto de
     * Spring. Se llama una sola vez, al arrancar.
     */
    @Value("${security.aes.secret-key}")
    public void inicializar(String clave) {
        EncryptionKeyHolder.claveSecreta = clave; // NOSONAR java:S2696, ver javadoc del metodo
    }

    public static String getClaveSecreta() {
        if (claveSecreta == null) {
            throw new IllegalStateException(
                    "La clave de cifrado (security.aes.secret-key) aun no ha sido inicializada por Spring");
        }
        return claveSecreta;
    }
}
