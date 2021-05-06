package com.ciji.serenity.config;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Client {

    @Value("${token}")
    private String token;

    @Getter
    private GatewayDiscordClient client;

    public void init() {
        GatewayDiscordClient client = null;
        try {
            client = DiscordClientBuilder.create(token)
                    .build()
                    .login()
                    .block();
        } catch (NullPointerException e) {
            log.error("Be sure to use a valid bot token!", e);
        }

        this.client = client;
    }
}
