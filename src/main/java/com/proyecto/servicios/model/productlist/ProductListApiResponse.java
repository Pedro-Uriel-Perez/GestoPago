package com.proyecto.servicios.model.productlist;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.List;

/**
 * Respuesta XML completa de GET /sistema/service/getProductList.do:
 * <pre>
 * &lt;RESPONSE&gt;
 *   &lt;MENSAJE&gt;&lt;CODIGO&gt;01&lt;/CODIGO&gt;&lt;TEXTO&gt;...&lt;/TEXTO&gt;&lt;/MENSAJE&gt;
 *   &lt;PRODUCTOS&gt;
 *     &lt;producto idProducto="..." idServicio="..." servicio="..." producto="..." .../&gt;
 *   &lt;/PRODUCTOS&gt;
 * &lt;/RESPONSE&gt;
 * </pre>
 */
@Data
@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductListApiResponse {

    @XmlElement(name = "MENSAJE")
    private MensajeExternoDTO mensaje;

    @XmlElementWrapper(name = "PRODUCTOS")
    @XmlElement(name = "producto")
    private List<ProductoExternoDTO> productos;
}
