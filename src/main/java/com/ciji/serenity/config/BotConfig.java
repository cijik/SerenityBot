package com.ciji.serenity.config;

import com.ciji.serenity.config.listeners.BootListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@PropertySource({ "classpath:application.properties", "classpath:token.properties" }) //token is added in a separate property file as the "token" property
@Slf4j
public class BotConfig {

    @Value("${token}")
    private String token;
    
    @Bean
    public RedisTemplate<?, ?> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<?, ?> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        return template;
    }

    @Bean
    public BootListener bootListener() {
        return new BootListener();
    }
}
