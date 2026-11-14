package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Correos al cliente. Cada uno se manda en HTML (con el mismo estilo oscuro
 * y verde lima de las apps) y tambien en texto plano, para los clientes de
 * correo que no muestran HTML.
 *
 * El HTML usa tablas y estilos en linea a proposito: es lo unico que
 * respetan de forma consistente Gmail, Outlook y los clientes moviles.
 */
@Service
@Slf4j
public class MailServiceImpl implements MailService {

    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

    private static final String NOMBRE_REMITENTE = "GestoPago";

    private final JavaMailSender mailSender;

    @Value("${app.mail.remitente}")
    private String remitente;

    public MailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void enviarCodigoVerificacion(String destinatario, String codigo) {
        String texto = "Tu codigo de verificacion es: " + codigo
                + "\n\nEste codigo es valido por 5 minutos. Si no solicitaste esta operacion, ignora este correo.";

        String contenido = parrafo("Usa este código para confirmar tu pago en GestoPago:")
                + """
                <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:24px 0;">
                  <tr>
                    <td style="background:#1A1D23;border:1px solid #2A2E36;border-radius:16px;padding:18px 28px;
                               font-family:'Courier New',monospace;font-size:36px;font-weight:700;
                               letter-spacing:10px;color:#C8F25A;">%s</td>
                  </tr>
                </table>
                """.formatted(codigo)
                + parrafo("El código vence en <strong style=\"color:#F2F4F7;\">5 minutos</strong>.")
                + nota("Si no solicitaste esta operación, ignora este correo. Nadie de GestoPago te pedirá este código.");

        enviar(destinatario, "Codigo de verificacion GestoPago", "Confirma tu pago", contenido, texto);
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
        String origenConAcentos = "MOVIL".equals(dispositivo) ? "la aplicación móvil" : "la plataforma web";
        String cuando = fechaHora.format(FORMATO_FECHA_HORA);

        String texto = "Se inicio sesion en tu cuenta desde " + origen + " el "
                + cuando + " (hora del centro de Mexico)."
                + "\n\nTu otra sesion sigue activa. Si no fuiste tu, cambia tu contrasena"
                + " y cierra las sesiones abiertas.";

        String contenido = parrafo("Se inició sesión en tu cuenta mientras tenías otra sesión abierta.")
                + """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"
                       style="margin:24px 0;background:#1A1D23;border:1px solid #2A2E36;border-radius:16px;">
                  <tr><td style="padding:18px 20px;font-family:Arial,Helvetica,sans-serif;">
                    <div style="font-size:13px;color:#A3ABB8;">Dispositivo</div>
                    <div style="font-size:17px;font-weight:700;color:#F2F4F7;padding-bottom:14px;">%s</div>
                    <div style="font-size:13px;color:#A3ABB8;">Fecha y hora (centro de México)</div>
                    <div style="font-size:17px;font-weight:700;color:#F2F4F7;">%s</div>
                  </td></tr>
                </table>
                """.formatted(capitalizar(origenConAcentos), cuando)
                + parrafo("Tu otra sesión sigue activa, puedes usar las dos a la vez.")
                + nota("¿No fuiste tú? Cambia tu contraseña y cierra las sesiones abiertas desde tu perfil.");

        enviar(destinatario, "Nuevo inicio de sesion en tu cuenta GestoPago", "Nuevo inicio de sesión", contenido, texto);
        log.info("Alerta de inicio de sesion enviada por correo. dispositivo={}", dispositivo);
    }

    private void enviar(String destinatario, String asunto, String titulo, String contenidoHtml, String textoPlano) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, true, StandardCharsets.UTF_8.name());
            ayudante.setFrom(remitente, NOMBRE_REMITENTE);
            ayudante.setTo(destinatario);
            ayudante.setSubject(asunto);
            ayudante.setText(textoPlano, plantilla(titulo, contenidoHtml));
            mailSender.send(mensaje);
        } catch (MessagingException | UnsupportedEncodingException e) {
            // Se convierte a MailException para que GlobalExceptionHandler lo
            // atienda igual que cualquier otra falla de correo (503).
            throw new MailPreparationException("No fue posible armar el correo", e);
        }
    }

    private static String plantilla(String titulo, String contenido) {
        return """
                <!doctype html>
                <html lang="es">
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#08090B;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#08090B;">
                  <tr><td align="center" style="padding:32px 16px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"
                           style="max-width:480px;background:#111317;border:1px solid #2A2E36;border-radius:24px;">
                      <tr><td style="padding:28px 28px 0 28px;font-family:Arial,Helvetica,sans-serif;">
                        <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                          <td style="width:36px;height:36px;background:#C8F25A;border-radius:18px;text-align:center;
                                     vertical-align:middle;font-size:18px;font-weight:700;color:#111317;">G</td>
                          <td style="padding-left:10px;font-size:18px;font-weight:700;color:#F2F4F7;">GestoPago</td>
                        </tr></table>
                      </td></tr>
                      <tr><td style="padding:28px;font-family:Arial,Helvetica,sans-serif;">
                        <h1 style="margin:0 0 12px 0;font-size:26px;line-height:1.2;color:#F2F4F7;">%s</h1>
                        %s
                      </td></tr>
                    </table>
                    <p style="margin:16px 0 0 0;font-family:Arial,Helvetica,sans-serif;font-size:12px;color:#6B7280;">
                      Este es un correo automático de GestoPago, no lo respondas.
                    </p>
                  </td></tr>
                </table>
                </body>
                </html>
                """.formatted(titulo, contenido);
    }

    private static String parrafo(String html) {
        return "<p style=\"margin:0 0 8px 0;font-size:15px;line-height:1.5;color:#A3ABB8;\">" + html + "</p>";
    }

    private static String nota(String html) {
        return "<p style=\"margin:20px 0 0 0;padding-top:16px;border-top:1px solid #2A2E36;font-size:13px;"
                + "line-height:1.5;color:#A3ABB8;\">" + html + "</p>";
    }

    private static String capitalizar(String texto) {
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
