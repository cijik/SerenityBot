package com.ciji.serenity.commands;

import com.ciji.serenity.config.Client;
import discord4j.core.object.command.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.ciji.serenity.enums.Commands.UPDATE_CHARACTER;

@Component
@Slf4j
@AllArgsConstructor
public class UpdateCharacterCommand implements SerenityCommand {

    private final Client client;

    @Override
    public void register() {
        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
                .name(UPDATE_CHARACTER.getCommand())
                .description("Adds a character sheet to the database")
                .type(ApplicationCommand.Type.CHAT_INPUT.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("Name of the character to add")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("owner-id")
                        .description("User ID of the character's owner")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .build();

        RestClient restClient = client.getClient().getRestClient();

        long applicationId = restClient.getApplicationId().block();

        restClient.getApplicationService()
                .createGuildApplicationCommand(applicationId, 177794959854796801L, commandRequest)
                .doOnError(e -> log.warn("Unable to create guild command", e))
                .onErrorResume(_ -> Mono.empty())
                .block();
    }
}
