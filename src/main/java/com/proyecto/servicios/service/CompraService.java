package com.proyecto.servicios.service;

import com.proyecto.servicios.model.compras.CompraConfirmarRequest;
import com.proyecto.servicios.model.compras.CompraIniciadaResponse;
import com.proyecto.servicios.model.compras.CompraRequest;
import com.proyecto.servicios.model.compras.CompraResultadoResponse;

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
}
