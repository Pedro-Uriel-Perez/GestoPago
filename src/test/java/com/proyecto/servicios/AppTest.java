package com.proyecto.servicios;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class AppTest {

    /**
     * Se simula SpringApplication para comprobar que main arranca la
     * aplicacion con la clase App sin levantar de verdad el servidor (que
     * necesitaria la base de datos, Redis y el correo reales).
     */
    @Test
    void main_arrancaSpringBootConLaClaseApp() {
        String[] argumentos = {"--server.port=0"};

        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            App.main(argumentos);

            spring.verify(() -> SpringApplication.run(App.class, argumentos));
        }
    }
}
