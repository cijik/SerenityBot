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
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;

@Configuration
@PropertySource(value = { "classpath:application.properties", "classpath:token.properties", "file:./token.properties" }, ignoreResourceNotFound = true) //token is added in a separate property file as the "token" property
@Slf4j
@EnableCaching
public class BotConfig {

    /**
     * Writes a "@class" type property so cached values are read back as their real type,
     * not as LinkedHashMap. The validator limits which classes may be created from Redis data.
     */
    private static GenericJacksonJsonRedisSerializer cacheValueSerializer() {
        return GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.ciji.serenity.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.lang.")
                        .build())
                .build();
    }
    
    @Bean
    public RedisTemplate<?, ?> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<?, ?> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(cacheValueSerializer());

        return template;
    }

    @Bean
    public BootListener bootListener() {
        return new BootListener();
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheConfiguration() {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(12))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(cacheValueSerializer()));

        return (builder) -> builder
                .withCacheConfiguration("sheets", defaults)
                .withCacheConfiguration("sheet-ranges", defaults);
    }
}
