package com.proyecto.servicios.service;

import java.time.LocalDateTime;

public interface MailService {

    void enviarCodigoVerificacion(String destinatario, String codigo);

    /**
     * Avisa al cliente que se inicio sesion en su cuenta desde un dispositivo
     * (WEB o MOVIL) mientras ya tenia otra sesion abierta.
     */
    void enviarAlertaInicioSesion(String destinatario, String dispositivo, LocalDateTime fechaHora);
}
