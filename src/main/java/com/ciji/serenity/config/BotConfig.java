package com.ciji.serenity.config;

import com.ciji.serenity.config.listeners.BootListener;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource({ "classpath:application.properties", "classpath:token.properties" }) //token is added in a separate property file as the "token" property
@Slf4j
public class BotConfig {

    @Value("${token}")
    private String token;
    
    @Bean
    @ConfigurationProperties("spring.datasource")
    public PGSimpleDataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(PGSimpleDataSource.class)
                .build();
    }

    @Bean
    public BootListener bootListener() {
        return new BootListener();
    }
}
