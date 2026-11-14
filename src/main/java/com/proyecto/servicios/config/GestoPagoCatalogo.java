package com.proyecto.servicios.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Datos del catalogo que GestoPago no manda dentro de getProductList.do pero
 * si documenta en su especificacion: el nombre de cada categoria
 * (tabla "idCatTipoServicio") y las imagenes publicas por servicio
 * (getServiceLogo / getReferenceImage).
 */
@Component
public class GestoPagoCatalogo {

    private static final String CATEGORIA_SIN_CLASIFICAR = "Otros";

    private static final Map<Integer, String> CATEGORIAS = Map.ofEntries(
            Map.entry(1, "Movistar"),
            Map.entry(2, "Telcel"),
            Map.entry(3, "AT&T / Iusacell"),
            Map.entry(4, "Unefon"),
            Map.entry(5, "Pago de Servicios"),
            Map.entry(6, "Nextel"),
            Map.entry(7, "Impuestos y Gobierno"),
            Map.entry(8, "Productos Financieros"),
            Map.entry(9, "Productos por Catalogo"),
            Map.entry(10, "Servicios de Prepago"),
            Map.entry(11, "Entretenimiento"),
            Map.entry(12, "Virgin"),
            Map.entry(13, "Otras Recargas"),
            Map.entry(14, "Venta de Seguros"),
            Map.entry(15, "Pago de Agua"),
            Map.entry(16, "Juegos y Sorteos"),
            Map.entry(18, "Autopistas y Transportes"));

    private final String urlImagenes;

    public GestoPagoCatalogo(@Value("${gestopago.imagenes.url}") String urlImagenes) {
        this.urlImagenes = urlImagenes;
    }

    public String categoria(Integer idCatTipoServicio) {
        return Optional.ofNullable(idCatTipoServicio)
                .map(CATEGORIAS::get)
                .orElse(CATEGORIA_SIN_CLASIFICAR);
    }

    /** Logotipo del proveedor (png). Null si el producto no trae idServicio. */
    public String logoUrl(Integer idServicio) {
        return Optional.ofNullable(idServicio)
                .map(id -> urlImagenes + "/servicios/" + id + ".png")
                .orElse(null);
    }

    /** Imagen de ayuda que muestra donde viene la referencia en el recibo. */
    public String imagenReferenciaUrl(Integer idServicio) {
        return Optional.ofNullable(idServicio)
                .map(id -> urlImagenes + "/referencias/" + id + ".jpg")
                .orElse(null);
    }
}
