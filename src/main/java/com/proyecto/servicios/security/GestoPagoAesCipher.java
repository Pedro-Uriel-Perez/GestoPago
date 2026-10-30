package com.proyecto.servicios.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cifra el payload de sendTx/confirmTx con AES-256-CBC usando la llave e IV
 * fijos que entrega PuntoRed (documentacion "Encryption Snipplets"), no el
 * esquema de AesEncryptionUtil (que genera un IV aleatorio por mensaje para
 * cifrar campos propios en la base de datos). PuntoRed exige reusar siempre
 * la MISMA llave/IV que ellos proveen, asi que es un cifrado distinto con un
 * proposito distinto (hablar su protocolo, no proteger datos propios).
 */
@Component
public class GestoPagoAesCipher {

    private final SecretKeySpec secretKey;
    private final IvParameterSpec iv;

    public GestoPagoAesCipher(@Value("${gestopago.sendtx.secret-key}") String secretKeyBase64,
                               @Value("${gestopago.sendtx.iv}") String ivBase64) {
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
        byte[] ivBytes = Base64.getDecoder().decode(ivBase64);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.iv = new IvParameterSpec(ivBytes);
    }

    public String encriptar(String textoPlano) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            return Base64.getEncoder().encodeToString(cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible cifrar el payload para GestoPago", e);
        }
    }

    public String desencriptar(String textoCifrado) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            return new String(cipher.doFinal(Base64.getDecoder().decode(textoCifrado)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible descifrar el payload de GestoPago", e);
        }
    }
}
