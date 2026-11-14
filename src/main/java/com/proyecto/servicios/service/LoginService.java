package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.model.clientes.SesionResponse;

import java.util.List;

public interface LoginService {

    /**
     * Crea el registro de seguridad/login del cliente al momento del
     * registro, guardando el hash (BCrypt) de su contrasena. Se separa de
     * iniciarSesion porque debe existir desde el registro, no hasta el
     * primer login.
     */
    void registrarCredenciales(Cliente cliente, String passwordPlano);

    /**
     * Verifica correo + contrasena contra el hash guardado, abre una sesion
     * para el dispositivo indicado (WEB o MOVIL; null = WEB) y emite su JWT.
     * La sesion del otro dispositivo sigue activa; si ya habia alguna sesion
     * abierta, se le avisa al cliente por correo del nuevo inicio de sesion.
     */
    String iniciarSesion(String correoElectronico, String passwordPlano, String dispositivo);

    /**
     * Cierra solo la sesion indicada (la del dispositivo que lo pide), sin
     * afectar la sesion del otro dispositivo.
     */
    void cerrarSesion(Integer clienteId, String sesionId);

    /**
     * Cierra todas las sesiones del cliente (web y movil), por ejemplo al
     * darlo de baja.
     */
    void cerrarTodasLasSesiones(Integer clienteId);

    /**
     * Sesiones activas del cliente, marcando cual es la que hace la consulta.
     */
    List<SesionResponse> obtenerSesionesActivas(Integer clienteId, String sesionActualId);

    /**
     * Cierra las sesiones cuyo ultimo acceso supera el umbral de inactividad
     * configurado. Corre en una tarea programada.
     */
    void cerrarSesionesInactivas();
}
