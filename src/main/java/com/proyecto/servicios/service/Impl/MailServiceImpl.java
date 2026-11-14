package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class MailServiceImpl implements MailService {

    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

    private final JavaMailSender mailSender;

    @Value("${app.mail.remitente}")
    private String remitente;

    public MailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void enviarCodigoVerificacion(String destinatario, String codigo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("Codigo de verificacion GestoPago");
        mensaje.setText("Tu codigo de verificacion es: " + codigo
                + "\n\nEste codigo es valido por 5 minutos. Si no solicitaste esta operacion, ignora este correo.");
        mailSender.send(mensaje);
        log.info("Codigo de verificacion enviado por correo");
    }

    /**
     * @Async: el aviso es informativo y no debe retrasar el inicio de
     * sesion; se manda en otro hilo. Si el envio falla, Spring lo deja en el
     * log y el cliente entra igual.
     */
    @Override
    @Async
    public void enviarAlertaInicioSesion(String destinatario, String dispositivo, LocalDateTime fechaHora) {
        String origen = "MOVIL".equals(dispositivo) ? "la aplicacion movil" : "la plataforma web";
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("Nuevo inicio de sesion en tu cuenta GestoPago");
        mensaje.setText("Se inicio sesion en tu cuenta desde " + origen + " el "
                + fechaHora.format(FORMATO_FECHA_HORA) + " (hora del centro de Mexico)."
                + "\n\nTu otra sesion sigue activa. Si no fuiste tu, cambia tu contrasena"
                + " y cierra las sesiones abiertas.");
        mailSender.send(mensaje);
        log.info("Alerta de inicio de sesion enviada por correo. dispositivo={}", dispositivo);
    }
}
