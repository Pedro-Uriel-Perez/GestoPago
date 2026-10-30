package com.proyecto.servicios.service;

public interface MailService {

    void enviarCodigoVerificacion(String destinatario, String codigo);
}
