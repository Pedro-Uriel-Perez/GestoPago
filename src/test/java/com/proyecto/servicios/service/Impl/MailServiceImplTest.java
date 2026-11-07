package com.proyecto.servicios.service.Impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailServiceImpl mailService;

    @Test
    void enviarCodigoVerificacion_mandaElPinAlDestinatarioDesdeLaCuentaConfigurada() {
        ReflectionTestUtils.setField(mailService, "remitente", "remitente@correo.com");

        mailService.enviarCodigoVerificacion("cliente@correo.com", "123456");

        ArgumentCaptor<SimpleMailMessage> mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mensaje.capture());
        assertThat(mensaje.getValue().getFrom()).isEqualTo("remitente@correo.com");
        assertThat(mensaje.getValue().getTo()).containsExactly("cliente@correo.com");
        assertThat(mensaje.getValue().getSubject()).isEqualTo("Codigo de verificacion GestoPago");
        assertThat(mensaje.getValue().getText()).contains("123456").contains("5 minutos");
    }
}
