package com.proyecto.servicios.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.nio.charset.StandardCharsets;

/**
 * Cifra/descifra un campo String de texto plano <-> texto cifrado (Base64)
 * en la base de datos. Se aplica explicitamente con @Convert en cada campo
 * que debe cifrarse (no autoApply, para no afectar por accidente otros
 * Strings del proyecto que no deben cifrarse).
 */
@Converter
public class AesStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String atributo) {
        return atributo == null ? null : AesEncryptionUtil.cifrar(atributo.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String convertToEntityAttribute(String dato) {
        return dato == null ? null : new String(AesEncryptionUtil.descifrar(dato), StandardCharsets.UTF_8);
    }
}
