package com.ciji.serenity.config;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.util.retry.Retry;

import java.time.Duration;

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
                    .gateway()
                    .setEnabledIntents(IntentSet.of(
                            Intent.GUILDS,
                            Intent.GUILD_MESSAGES,
                            Intent.GUILD_PRESENCES,
                            Intent.MESSAGE_CONTENT
                    ))
                    .setInitialPresence(s -> ClientPresence.online(ClientActivity.playing("Starting...")))
                    .login()
                    .retryWhen(Retry.backoff(5, Duration.ofSeconds(5))
                            .maxBackoff(Duration.ofMinutes(2))
                            .doBeforeRetry(signal ->
                                    log.warn("Retrying connection to Discord. Attempt: {}", signal.totalRetries() + 1)))

                    .block();
        } catch (NullPointerException e) {
            log.error("Be sure to use a valid bot token!", e);
        }

        if (client != null) {
            client.onDisconnect()
                    .doOnTerminate(() -> log.warn("Disconnected from Discord Gateway"))
                    .subscribe();
        }

        this.client = client;
    }

    @PreDestroy
    public void onShutdown() {
        if (client != null) {
            client.logout().block();
        }
    }
}
