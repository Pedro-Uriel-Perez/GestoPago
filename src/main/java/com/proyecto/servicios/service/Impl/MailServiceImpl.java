package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
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
}
