package com.proyecto.servicios.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AesEncryptionUtilTest {

    @BeforeAll
    static void inicializarClave() {
        new EncryptionKeyHolder().inicializar("clave-de-prueba-para-unit-tests-2026");
    }

    @Test
    void cifrarYDescifrar_textoPlano_recuperaElValorOriginal() {
        String textoOriginal = "cliente@correo.com";

        String textoCifrado = AesEncryptionUtil.cifrar(textoOriginal.getBytes(StandardCharsets.UTF_8));
        String textoDescifrado = new String(AesEncryptionUtil.descifrar(textoCifrado), StandardCharsets.UTF_8);

        assertThat(textoCifrado).isNotEqualTo(textoOriginal);
        assertThat(textoDescifrado).isEqualTo(textoOriginal);
    }

    @Test
    void cifrarDosVeces_elMismoTexto_produceResultadosDistintos() {
        String textoOriginal = "dato-sensible";

        String primerCifrado = AesEncryptionUtil.cifrar(textoOriginal.getBytes(StandardCharsets.UTF_8));
        String segundoCifrado = AesEncryptionUtil.cifrar(textoOriginal.getBytes(StandardCharsets.UTF_8));

        assertThat(primerCifrado).isNotEqualTo(segundoCifrado);
    }

    @Test
    void converterDeString_cifraYDescifraCorrectamente() {
        AesStringConverter converter = new AesStringConverter();
        String original = "Juan Perez";

        String columna = converter.convertToDatabaseColumn(original);
        String recuperado = converter.convertToEntityAttribute(columna);

        assertThat(columna).isNotEqualTo(original);
        assertThat(recuperado).isEqualTo(original);
    }

    @Test
    void converterDeArregloDeDoubles_cifraYDescifraCorrectamente() {
        AesDoubleArrayConverter converter = new AesDoubleArrayConverter();
        double[] original = {0.1234, -0.5678, 1.0, 0.0, 99.999};

        String columna = converter.convertToDatabaseColumn(original);
        double[] recuperado = converter.convertToEntityAttribute(columna);

        assertThat(recuperado).containsExactly(original);
    }

    @Test
    void converters_conValorNulo_retornanNulo() {
        AesStringConverter stringConverter = new AesStringConverter();
        AesDoubleArrayConverter arrayConverter = new AesDoubleArrayConverter();

        assertThat(stringConverter.convertToDatabaseColumn(null)).isNull();
        assertThat(stringConverter.convertToEntityAttribute(null)).isNull();
        assertThat(arrayConverter.convertToDatabaseColumn(null)).isNull();
        assertThat(arrayConverter.convertToEntityAttribute(null)).isNull();
    }
}
