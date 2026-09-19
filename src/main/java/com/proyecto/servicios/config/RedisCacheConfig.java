package com.proyecto.servicios.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis se usa como cache de lectura delante de la tabla gestopago_productos
 * (fuente de verdad en Postgres, ver GestoPagoProductListSyncServiceImpl).
 * El TTL es una red de seguridad adicional a la invalidacion explicita
 * (@CacheEvict) que ocurre en cada sincronizacion diaria.
 *
 * Los valores se serializan como JSON (no con el serializador nativo de
 * Java, que exigiria que cada DTO cacheado implemente Serializable). El
 * ObjectMapper propio registra JavaTimeModule para poder serializar
 * LocalDateTime (fechaActualizacion) y mantiene el tipado por defecto de
 * GenericJackson2JsonRedisSerializer para poder deserializar de vuelta al
 * tipo concreto (ProductoResponse).
 */
@Configuration
public class RedisCacheConfig {

    @Value("${gestopago.productos.cache-ttl-hours:25}")
    private long cacheTtlHours;

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        ObjectMapper redisObjectMapper = new ObjectMapper();
        redisObjectMapper.registerModule(new JavaTimeModule());
        redisObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        redisObjectMapper.activateDefaultTyping(
                redisObjectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        RedisCacheConfiguration configuracion = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(cacheTtlHours))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(redisObjectMapper)));

        return builder -> builder.withCacheConfiguration("productos", configuracion);
    }
}
