package com.proyecto.servicios.config;

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
 */
@Configuration
public class RedisCacheConfig {

    @Value("${gestopago.productos.cache-ttl-hours:25}")
    private long cacheTtlHours;

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        RedisCacheConfiguration configuracion = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(cacheTtlHours))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return builder -> builder.withCacheConfiguration("productos", configuracion);
    }
}
