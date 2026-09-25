package com.example.customerprofile.config;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;

// Turns on @Cacheable / @CachePut / @CacheEvict. Redis itself is configured in application.properties.
@Configuration
@EnableCaching
public class CacheConfig {

    // By default, Spring Data Redis may send cache writes in the background without waiting for them.
    // Then an older value could land in Redis *after* a newer one. "Immediate writes" makes every
    // cache write finish before the method returns, so an update always wins.
    @Bean
    RedisCacheManagerBuilderCustomizer immediateCacheWrites(RedisConnectionFactory connectionFactory) {
        return builder -> builder.cacheWriter(
                RedisCacheWriter.create(connectionFactory, writer -> writer.immediateWrites()));
    }
}
