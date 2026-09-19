package com.proyecto.servicios.model.productlist;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

/**
 * Corresponde a cada nodo &lt;producto ...&gt;...&lt;/producto&gt; dentro de
 * &lt;PRODUCTOS&gt; en la respuesta de GET /sistema/service/getProductList.do
 * (especificacion PuntoRed/GestoPago, tabla "Detailed Response XML Data").
 * Los datos del producto vienen como atributos XML, no como elementos.
 * precio se modela como String porque la especificacion lo tipa como
 * string(15), no como numero.
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductoExternoDTO {

    @XmlAttribute(name = "idProducto")
    private Integer idProducto;

    @XmlAttribute(name = "idServicio")
    private Integer idServicio;

    @XmlAttribute(name = "servicio")
    private String servicio;

    @XmlAttribute(name = "producto")
    private String producto;

    @XmlAttribute(name = "idCatTipoServicio")
    private Integer idCatTipoServicio;

    @XmlAttribute(name = "tipoFront")
    private Integer tipoFront;

    @XmlAttribute(name = "hasDigitoVerificador")
    private Boolean hasDigitoVerificador;

    @XmlAttribute(name = "tipoReferencia")
    private String tipoReferencia;

    @XmlAttribute(name = "precio")
    private String precio;

    @XmlElement(name = "legend")
    private String legend;
}
