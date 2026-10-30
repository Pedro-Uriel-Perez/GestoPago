package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import com.proyecto.servicios.model.productlist.MensajeExternoDTO;
import lombok.Data;

/**
 * Respuesta XML real de sendTx.do/confirmTx.do, confirmada con una llamada
 * de prueba directa contra el sandbox (2026-10-24):
 * <pre>
 * &lt;RESPONSE&gt;
 *   &lt;ID_TX&gt;...&lt;/ID_TX&gt;&lt;NUM_AUTORIZACION&gt;...&lt;/NUM_AUTORIZACION&gt;
 *   &lt;SALDO&gt;...&lt;/SALDO&gt;&lt;COMISION&gt;...&lt;/COMISION&gt;
 *   &lt;SALDO_F&gt;...&lt;/SALDO_F&gt;&lt;COMISION_F&gt;...&lt;/COMISION_F&gt;
 *   &lt;FECHA&gt;...&lt;/FECHA&gt;&lt;MONTO&gt;...&lt;/MONTO&gt;
 *   &lt;MENSAJE&gt;&lt;CODIGO&gt;01&lt;/CODIGO&gt;&lt;TEXTO&gt;...&lt;/TEXTO&gt;&lt;REFERENCIA/&gt;&lt;/MENSAJE&gt;
 * &lt;/RESPONSE&gt;
 * </pre>
 * CODIGO/TEXTO/REFERENCIA van anidados dentro de MENSAJE, igual que en
 * getProductList.do (se reusa MensajeExternoDTO). La especificacion los
 * listaba como campos sueltos en su tabla de campos, pero la llamada real
 * demostro que van anidados.
 */
@Data
@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoSenTxResponse {

    @XmlElement(name = "MENSAJE")
    private MensajeExternoDTO mensaje;

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

    @XmlElement(name = "PIN")
    private String pin;

    @XmlElement(name = "legend")
    private String legend;

    public boolean esExitoso() {
        return mensaje != null && mensaje.esExitoso();
    }
}
