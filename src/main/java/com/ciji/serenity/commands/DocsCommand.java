package com.ciji.serenity.commands;

import static com.ciji.serenity.enums.Command.DOCS;

import org.springframework.stereotype.Component;

import com.ciji.serenity.config.Client;

import discord4j.core.object.command.ApplicationCommand;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@AllArgsConstructor
public class DocsCommand implements SerenityCommand {

    @Override
    public void register(long applicationId, RestClient restClient) {
        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
                .name(DOCS.getCommand())
                .description(DOCS.getShortDesc())
                .type(ApplicationCommand.Type.CHAT_INPUT.getValue())
                .build();

        restClient.getApplicationService()
                .createGlobalApplicationCommand(applicationId, commandRequest)
                .doOnError(e -> log.error("Unable to create guild command", e))
                .onErrorResume(_ -> Mono.empty())
                .block();
    }
}
