package com.ciji.serenity.commands;

import com.ciji.serenity.config.Client;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@AllArgsConstructor
public class StartTimerCommand implements SerenityCommand {

    private final Client client;

    @Override
    public void register() {
        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
                .name("startTimer")
                .description("Start a timer.")
                .build();

        RestClient restClient = client.getClient().getRestClient();

        long applicationId = restClient.getApplicationId().block();

        restClient.getApplicationService()
                .createGuildApplicationCommand(applicationId, 177794959854796801L, commandRequest)
                .doOnError(e -> log.warn("Unable to create guild command", e))
                .onErrorResume(e -> Mono.empty())
                .block();
    }
}
