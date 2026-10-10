package com.proyecto.servicios.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.nio.ByteBuffer;

/**
 * Cifra/descifra el vector de datos biometricos (double[], ej. un embedding
 * facial) <-> texto cifrado (Base64) en la base de datos. El arreglo se
 * serializa a bytes antes de cifrarlo, ya que un valor cifrado deja de ser
 * un numero legible por Postgres: la columna en base de datos es TEXT.
 */
@Converter
public class AesDoubleArrayConverter implements AttributeConverter<double[], String> {

    @Override
    public String convertToDatabaseColumn(double[] atributo) {
        if (atributo == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES * atributo.length);
        for (double valor : atributo) {
            buffer.putDouble(valor);
        }
        return AesEncryptionUtil.cifrar(buffer.array());
    }

    @Override
    public double[] convertToEntityAttribute(String dato) {
        if (dato == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(AesEncryptionUtil.descifrar(dato));
        double[] valores = new double[buffer.remaining() / Double.BYTES];
        for (int i = 0; i < valores.length; i++) {
            valores[i] = buffer.getDouble();
        }
        return valores;
    }
}
