package com.proyecto.servicios.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GestoPagoCatalogoTest {

    private final GestoPagoCatalogo catalogo = new GestoPagoCatalogo("https://imagenes.test");

    @Test
    void categoria_traduceElIdDeLaTablaDeGestoPago() {
        assertThat(catalogo.categoria(2)).isEqualTo("Telcel");
        assertThat(catalogo.categoria(5)).isEqualTo("Pago de Servicios");
        assertThat(catalogo.categoria(15)).isEqualTo("Pago de Agua");
    }

    @Test
    void categoria_desconocidaOSinId_regresaOtros() {
        assertThat(catalogo.categoria(999)).isEqualTo("Otros");
        assertThat(catalogo.categoria(null)).isEqualTo("Otros");
    }

    @Test
    void logoUrl_armaLaRutaPublicaPorIdServicio() {
        assertThat(catalogo.logoUrl(79)).isEqualTo("https://imagenes.test/servicios/79.png");
        assertThat(catalogo.logoUrl(null)).isNull();
    }

    @Test
    void imagenReferenciaUrl_armaLaRutaDeLaImagenDeAyuda() {
        assertThat(catalogo.imagenReferenciaUrl(79)).isEqualTo("https://imagenes.test/referencias/79.jpg");
        assertThat(catalogo.imagenReferenciaUrl(null)).isNull();
    }
}
