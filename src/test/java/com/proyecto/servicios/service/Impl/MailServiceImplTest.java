package com.proyecto.servicios.service.Impl;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailServiceImpl mailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailService, "remitente", "remitente@correo.com");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    private MimeMessage mensajeEnviado() {
        ArgumentCaptor<MimeMessage> mensaje = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(mensaje.capture());
        return mensaje.getValue();
    }

    /** Las partes de texto del correo: [texto plano, html]. */
    private static List<String> partesDeTexto(MimeMessage mensaje) throws Exception {
        List<String> partes = new ArrayList<>();
        recolectar(mensaje.getContent(), partes);
        return partes;
    }

    private static void recolectar(Object contenido, List<String> partes) throws Exception {
        if (contenido instanceof Multipart multiparte) {
            for (int i = 0; i < multiparte.getCount(); i++) {
                BodyPart parte = multiparte.getBodyPart(i);
                recolectar(parte.getContent(), partes);
            }
        } else if (contenido instanceof String texto) {
            partes.add(texto);
        }
    }

    @Test
    void enviarCodigoVerificacion_mandaElPinEnHtmlYTextoPlanoDesdeGestoPago() throws Exception {
        mailService.enviarCodigoVerificacion("cliente@correo.com", "123456");

        MimeMessage mensaje = mensajeEnviado();
        InternetAddress de = (InternetAddress) mensaje.getFrom()[0];
        assertThat(de.getAddress()).isEqualTo("remitente@correo.com");
        assertThat(de.getPersonal()).isEqualTo("GestoPago");
        assertThat(((InternetAddress) mensaje.getAllRecipients()[0]).getAddress()).isEqualTo("cliente@correo.com");
        assertThat(mensaje.getSubject()).isEqualTo("Codigo de verificacion GestoPago");

        List<String> partes = partesDeTexto(mensaje);
        assertThat(partes).hasSize(2);
        assertThat(partes.get(0)).contains("123456").contains("5 minutos").doesNotContain("<");
        assertThat(partes.get(1)).contains("<html").contains("123456").contains("Confirma tu pago");
    }

    @Test
    void enviarAlertaInicioSesion_avisaDesdeQueDispositivoYCuando() throws Exception {
        mailService.enviarAlertaInicioSesion("cliente@correo.com", "MOVIL", LocalDateTime.of(2026, 11, 13, 10, 42));

        MimeMessage mensaje = mensajeEnviado();
        assertThat(mensaje.getSubject()).isEqualTo("Nuevo inicio de sesion en tu cuenta GestoPago");
        List<String> partes = partesDeTexto(mensaje);
        assertThat(partes.get(0)).contains("la aplicacion movil").contains("13/11/2026 a las 10:42");
        assertThat(partes.get(1)).contains("La aplicación móvil").contains("13/11/2026 a las 10:42");
    }

    @Test
    void enviarAlertaInicioSesion_desdeWeb_mencionaLaPlataformaWeb() throws Exception {
        mailService.enviarAlertaInicioSesion("cliente@correo.com", "WEB", LocalDateTime.of(2026, 11, 13, 10, 42));

        List<String> partes = partesDeTexto(mensajeEnviado());
        assertThat(partes.get(0)).contains("la plataforma web");
        assertThat(partes.get(1)).contains("La plataforma web");
    }
}
