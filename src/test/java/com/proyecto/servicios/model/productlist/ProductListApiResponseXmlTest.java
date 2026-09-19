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
 * de una llamada HTTP real. El XML de este test es un extracto real de esa
 * documentacion (incluye precio y tipoReferencia).
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
                    <producto servicio='AGUAKAN (Cancun)' producto='Agua Cancun (Mun. de Benito Juarez y de Isla Mujeres)' idServicio='56' idProducto='185' idCatTipoServicio='15' tipoFront='2' hasDigitoVerificador='false' precio='10.0' showAyuda='false' tipoReferencia='c'>
                        <legend>
                            <![CDATA[Para cualquier duda o aclaracion con tu pago, comunicate al servicio de Atencion a clientes de AGUAKAN al telefono 073. No olvides consevar tu comprobante de pago.]]>
                        </legend>
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
        assertThat(producto.getIdProducto()).isEqualTo(185);
        assertThat(producto.getIdServicio()).isEqualTo(56);
        assertThat(producto.getIdCatTipoServicio()).isEqualTo(15);
        assertThat(producto.getTipoFront()).isEqualTo(2);
        assertThat(producto.getServicio()).isEqualTo("AGUAKAN (Cancun)");
        assertThat(producto.getProducto()).isEqualTo("Agua Cancun (Mun. de Benito Juarez y de Isla Mujeres)");
        assertThat(producto.getHasDigitoVerificador()).isFalse();
        assertThat(producto.getTipoReferencia()).isEqualTo("c");
        assertThat(producto.getPrecio()).isEqualTo("10.0");
        assertThat(producto.getLegend()).contains("AGUAKAN");
    }
}
