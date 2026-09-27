package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.clientes.Cliente;

public interface LoginService {

    /**
     * Crea el registro de seguridad/login del cliente al momento del
     * registro, guardando el hash (BCrypt) de su contrasena. Se separa de
     * iniciarSesion porque debe existir desde el registro, no hasta el
     * primer login.
     */
    void registrarCredenciales(Cliente cliente, String passwordPlano);

    /**
     * Verifica correo + contrasena contra el hash guardado, emite un JWT y
     * marca la sesion como activa.
     */
    String iniciarSesion(String correoElectronico, String passwordPlano);

    void cerrarSesion(Integer clienteId);

    /**
     * Cierra (sesionActiva = false) las sesiones cuyo ultimo acceso supera
     * el umbral de inactividad configurado. Corre en una tarea programada.
     */
    void cerrarSesionesInactivas();
}
