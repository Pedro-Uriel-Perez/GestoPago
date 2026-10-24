package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * Respuesta XML de sendTx.do/confirmTx.do segun la tabla "The XML Response"
 * de la especificacion PuntoRed. CODIGO distinto de "01" es error (ver
 * esExitoso()). NO VERIFICADO aun contra una llamada real: la especificacion
 * solo muestra esta forma plana en la tabla de campos; un ejemplo aislado de
 * timeout mostraba CODIGO anidado dentro de un nodo MENSAJE en su lugar. Hay
 * que confirmar la forma real con la primera llamada de prueba contra el
 * sandbox y ajustar si hace falta.
 */
@Data
@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoSenTxResponse {

    @XmlElement(name = "CODIGO")
    private String codigo;

    @XmlElement(name = "ID_TX")
    private String idTx;

    @XmlElement(name = "NUM_AUTORIZACION")
    private String numAutorizacion;

    @XmlElement(name = "SALDO")
    private String saldo;

    @XmlElement(name = "SALDO_F")
    private String saldoFormateado;

    @XmlElement(name = "COMISION")
    private String comision;

    @XmlElement(name = "COMISION_F")
    private String comisionFormateada;

    @XmlElement(name = "FECHA")
    private String fecha;

    @XmlElement(name = "MONTO")
    private String monto;

    @XmlElement(name = "REFERENCIA")
    private String referencia;

    @XmlElement(name = "PIN")
    private String pin;

    @XmlElement(name = "legend")
    private String legend;

    @XmlElement(name = "TEXTO")
    private String texto;

    public boolean esExitoso() {
        return "01".equals(codigo);
    }
}
