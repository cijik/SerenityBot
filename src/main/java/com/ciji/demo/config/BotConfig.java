package com.ciji.demo.config;

import java.util.List;

import com.ciji.demo.listeners.EventListener;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import lombok.extern.slf4j.Slf4j;

@Configuration
@PropertySource({ "classpath:application.properties", "classpath:token.properties" }) //token is added in a separate property file as the "token" property
@Slf4j
public class BotConfig {

    @Value("${token}")
    private String token;
    
    @Bean
    @ConfigurationProperties("spring.datasource")
    public HikariDataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class)
                .build();
    }

    @Bean
    public <T extends Event> GatewayDiscordClient gatewayDiscordClient(List<EventListener<T>> eventListeners) {
        GatewayDiscordClient client = null;

        try {
            client = DiscordClientBuilder.create(token)
            .build()
            .login()
            .block();

            for(EventListener<T> listener : eventListeners) {
                client.on(listener.getEventType())
                    .flatMap(listener::execute)
                    .onErrorResume(listener::handleError)
                    .subscribe();
            }
        } catch (NullPointerException e) {
            log.error("Be sure to use a valid bot token!", e);
        }

        return client;
    }
}
