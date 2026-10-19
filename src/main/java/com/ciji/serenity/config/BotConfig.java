package com.ciji.serenity.config;

import com.ciji.serenity.config.listener.BootListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration
@PropertySource(value = { "classpath:application.properties", "classpath:token.properties", "file:./token.properties" }, ignoreResourceNotFound = true) //token is added in a separate property file as the "token" property
@Slf4j
@EnableCaching
public class BotConfig {
    
    @Bean
    public RedisTemplate<?, ?> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<?, ?> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJacksonJsonRedisSerializer(new ObjectMapper()));

        return template;
    }

    @Bean
    public BootListener bootListener() {
        return new BootListener();
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheConfiguration() {
        return (builder) -> builder
                .withCacheConfiguration("sheets", RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofHours(12))
                        .disableCachingNullValues()
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJacksonJsonRedisSerializer(new ObjectMapper()))))
                .withCacheConfiguration("sheet-ranges", RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofHours(12))
                        .disableCachingNullValues()
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJacksonJsonRedisSerializer(new ObjectMapper()))));
    }
}
