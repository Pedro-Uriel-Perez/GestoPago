package com.proyecto.servicios.model.productlist;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que ProductListApiResponse deserializa correctamente el XML real
 * documentado por PuntoRed para GET /sistema/service/getProductList.do
 * (https://documenter.getpostman.com/view/19876210/Uz5MFtdn), sin depender
 * de una llamada HTTP real.
 */
class ProductListApiResponseXmlTest {

    private static final String XML_RESPUESTA_GESTOPAGO = """
            <?xml version='1.0' encoding='UTF-8'?>
            <RESPONSE>
                <MENSAJE>
                    <CODIGO>01</CODIGO>
                    <TEXTO>Operacion realizada con exito</TEXTO>
                </MENSAJE>
                <PRODUCTOS>
                    <producto servicio="AGUAKAN (Cancun)" producto="Agua Cancun (Mun. de Benito Juarez)"
                              idServicio="12" idProducto="345" idCatTipoServicio="2" tipoFront="1"
                              hasDigitoVerificador="true">
                        <legend><![CDATA[Para cualquier aclaracion con tu pago, comunicate al call center.]]></legend>
                    </producto>
                </PRODUCTOS>
            </RESPONSE>
            """;

    @Test
    void deserializaElXmlRealDeGestoPagoCorrectamente() throws Exception {
        JAXBContext context = JAXBContext.newInstance(ProductListApiResponse.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();

        ProductListApiResponse respuesta = (ProductListApiResponse) unmarshaller.unmarshal(
                new StringReader(XML_RESPUESTA_GESTOPAGO));

        assertThat(respuesta.getMensaje().getCodigo()).isEqualTo("01");
        assertThat(respuesta.getMensaje().esExitoso()).isTrue();
        assertThat(respuesta.getProductos()).hasSize(1);

        ProductoExternoDTO producto = respuesta.getProductos().get(0);
        assertThat(producto.getIdProducto()).isEqualTo(345);
        assertThat(producto.getIdServicio()).isEqualTo(12);
        assertThat(producto.getServicio()).isEqualTo("AGUAKAN (Cancun)");
        assertThat(producto.getProducto()).isEqualTo("Agua Cancun (Mun. de Benito Juarez)");
        assertThat(producto.getHasDigitoVerificador()).isTrue();
        assertThat(producto.getLegend()).contains("call center");
    }
}
