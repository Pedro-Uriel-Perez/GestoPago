package com.proyecto.servicios.service;

import com.proyecto.servicios.model.compras.CompraConfirmarRequest;
import com.proyecto.servicios.model.compras.CompraIniciadaResponse;
import com.proyecto.servicios.model.compras.CompraRequest;
import com.proyecto.servicios.model.compras.CompraResultadoResponse;
import com.proyecto.servicios.model.compras.PagoResponse;
import com.proyecto.servicios.model.compras.ReferenciaVerificadaResponse;
import com.proyecto.servicios.model.compras.VerificarReferenciaRequest;

import java.util.List;

public interface CompraService {

    /**
     * Valida que la cuenta sea del cliente autenticado, que este activo, que
     * no tenga otra compra pendiente, que el producto exista y el monto
     * corresponda al catalogo, y que el saldo alcance. Congela el payload
     * junto con un PIN de 6 digitos en Redis (TTL corto) y manda el PIN por
     * correo. No llama a GestoPago todavia.
     */
    CompraIniciadaResponse iniciarCompra(CompraRequest request, Integer clienteIdAutenticado);

    /**
     * Valida que la operacion sea del cliente autenticado y el PIN, bloquea
     * la cuenta, vuelve a verificar saldo, ejecuta sendTx (con conciliacion
     * via confirmTx si hay timeout) y descuenta el saldo solo si GestoPago
     * confirma exito.
     */
    CompraResultadoResponse confirmarCompra(CompraConfirmarRequest request, Integer clienteIdAutenticado);

    /**
     * Valida con GestoPago (verifyReference.do) la referencia de un pago de
     * servicio antes de pagarlo. Si GestoPago no ofrece consulta en linea
     * para ese servicio regresa NO_VERIFICABLE.
     */
    ReferenciaVerificadaResponse verificarReferencia(VerificarReferenciaRequest request);

    /** Historial de pagos del cliente autenticado, del mas reciente al mas antiguo. */
    List<PagoResponse> obtenerHistorial(Integer clienteIdAutenticado);

    /** Detalle de un pago; 404 si no existe o no es del cliente autenticado. */
    PagoResponse obtenerPago(Integer pagoId, Integer clienteIdAutenticado);
}
