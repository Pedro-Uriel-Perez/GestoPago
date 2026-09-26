package com.proyecto.servicios.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifrado simetrico AES-256/GCM para los campos sensibles de la tabla
 * "logins" (correo, nombre, JWT, datos biometricos). La clave de la
 * especificacion (application.properties: security.aes.secret-key) se
 * deriva a una llave de 256 bits con PBKDF2, para no exigir que la
 * propiedad tenga un tamano exacto de bytes.
 *
 * El IV se genera aleatoriamente en cada cifrado y se antepone al texto
 * cifrado (formato: IV || CIPHERTEXT, todo en Base64) para poder
 * descifrarlo despues sin guardar el IV en una columna aparte.
 */
public final class AesEncryptionUtil {

    private static final String ALGORITMO_CIFRADO = "AES/GCM/NoPadding";
    private static final String ALGORITMO_DERIVACION_LLAVE = "PBKDF2WithHmacSHA256";
    private static final byte[] SAL_DERIVACION_LLAVE = "gestopago-clientes-salt".getBytes(StandardCharsets.UTF_8);
    private static final int ITERACIONES_DERIVACION_LLAVE = 65536;
    private static final int TAMANIO_LLAVE_BITS = 256;
    private static final int TAMANIO_TAG_GCM_BITS = 128;
    private static final int TAMANIO_IV_BYTES = 12;

    private AesEncryptionUtil() {
    }

    public static String cifrar(byte[] datos) {
        try {
            byte[] iv = new byte[TAMANIO_IV_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITMO_CIFRADO);
            cipher.init(Cipher.ENCRYPT_MODE, derivarLlave(), new GCMParameterSpec(TAMANIO_TAG_GCM_BITS, iv));
            byte[] textoCifrado = cipher.doFinal(datos);

            byte[] resultado = new byte[iv.length + textoCifrado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(textoCifrado, 0, resultado, iv.length, textoCifrado.length);

            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible cifrar la informacion", e);
        }
    }

    public static byte[] descifrar(String textoCifradoBase64) {
        try {
            byte[] datos = Base64.getDecoder().decode(textoCifradoBase64);
            byte[] iv = new byte[TAMANIO_IV_BYTES];
            byte[] textoCifrado = new byte[datos.length - TAMANIO_IV_BYTES];
            System.arraycopy(datos, 0, iv, 0, TAMANIO_IV_BYTES);
            System.arraycopy(datos, TAMANIO_IV_BYTES, textoCifrado, 0, textoCifrado.length);

            Cipher cipher = Cipher.getInstance(ALGORITMO_CIFRADO);
            cipher.init(Cipher.DECRYPT_MODE, derivarLlave(), new GCMParameterSpec(TAMANIO_TAG_GCM_BITS, iv));
            return cipher.doFinal(textoCifrado);
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible descifrar la informacion", e);
        }
    }

    private static SecretKeySpec derivarLlave() throws Exception {
        String claveSecreta = EncryptionKeyHolder.getClaveSecreta();
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITMO_DERIVACION_LLAVE);
        PBEKeySpec spec = new PBEKeySpec(
                claveSecreta.toCharArray(), SAL_DERIVACION_LLAVE, ITERACIONES_DERIVACION_LLAVE, TAMANIO_LLAVE_BITS);
        byte[] llave = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(llave, "AES");
    }
}
