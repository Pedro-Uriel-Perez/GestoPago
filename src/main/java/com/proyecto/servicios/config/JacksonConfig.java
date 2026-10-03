package com.proyecto.servicios.config;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Por defecto Jackson acepta numeros/booleanos mandados como string JSON
 * (ej. "ingresoMensual": "19000") y los convierte sin avisar. Eso permite
 * que datos que deberian ser numericos lleguen como texto sin que ninguna
 * validacion lo note. Se desactiva esa conversion implicita: un numero o
 * booleano entre comillas ahora falla la deserializacion (lo atrapa
 * HttpMessageNotReadableException en GlobalExceptionHandler, igual que una
 * fecha con formato invalido).
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer rechazarNumerosYBooleanosComoTexto() {
        return builder -> builder.postConfigurer(objectMapper -> {
            objectMapper.coercionConfigFor(LogicalType.Integer)
                    .setCoercion(CoercionInputShape.String, CoercionAction.Fail);
            objectMapper.coercionConfigFor(LogicalType.Float)
                    .setCoercion(CoercionInputShape.String, CoercionAction.Fail);
            objectMapper.coercionConfigFor(LogicalType.Boolean)
                    .setCoercion(CoercionInputShape.String, CoercionAction.Fail);
        });
    }
}
