package com.proyecto.servicios.model.productlist;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

/**
 * Corresponde al nodo &lt;MENSAJE&gt; de la respuesta XML de GestoPago:
 * &lt;MENSAJE&gt;&lt;CODIGO&gt;01&lt;/CODIGO&gt;&lt;TEXTO&gt;...&lt;/TEXTO&gt;&lt;/MENSAJE&gt;
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class MensajeExternoDTO {

    @XmlElement(name = "CODIGO")
    private String codigo;

    @XmlElement(name = "TEXTO")
    private String texto;

    public boolean esExitoso() {
        return "01".equals(codigo);
    }
}
