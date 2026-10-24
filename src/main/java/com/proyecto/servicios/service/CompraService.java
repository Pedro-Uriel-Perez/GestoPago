package com.proyecto.servicios.service;

import com.proyecto.servicios.model.compras.CompraConfirmarRequest;
import com.proyecto.servicios.model.compras.CompraIniciadaResponse;
import com.proyecto.servicios.model.compras.CompraRequest;
import com.proyecto.servicios.model.compras.CompraResultadoResponse;

public interface CompraService {

    /**
     * Valida cliente activo y saldo suficiente, arma el payload para
     * GestoPago, lo congela junto con un PIN de 6 digitos en Redis (TTL
     * corto) y manda el PIN por correo. No llama a GestoPago todavia.
     */
    CompraIniciadaResponse iniciarCompra(CompraRequest request);

    /**
     * Valida el PIN contra lo guardado en Redis y, si coincide, cifra el
     * payload congelado y ejecuta la compra real contra GestoPago (sendTx).
     */
    CompraResultadoResponse confirmarCompra(CompraConfirmarRequest request);
}
