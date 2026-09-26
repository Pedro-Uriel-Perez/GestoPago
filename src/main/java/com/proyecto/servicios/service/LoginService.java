package com.proyecto.servicios.service;

public interface LoginService {

    /**
     * Identifica al cliente por su correo, le emite un JWT y marca su
     * sesion como activa. Simplificacion documentada: el enunciado no
     * define un mecanismo de password para el login del cliente (el
     * registro tampoco captura una), asi que por ahora el correo
     * registrado es suficiente para identificarlo; el reconocimiento
     * facial (datos_biometricos) queda reservado para cuando se integre
     * una libreria real (ver docs/onboarding-clientes.md).
     */
    String iniciarSesion(String correoElectronico);

    void cerrarSesion(Integer clienteId);

    /**
     * Cierra (sesionActiva = false) las sesiones cuyo ultimo acceso supera
     * el umbral de inactividad configurado. Corre en una tarea programada.
     */
    void cerrarSesionesInactivas();
}
