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

import static com.ciji.serenity.enums.Commands.ROLL;

@Component
@Slf4j
@AllArgsConstructor
public class RollCommand implements SerenityCommand {

    private final Client client;

    @Override
    public void register() {
        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
                .name(ROLL.getCommand())
                .description(ROLL.getShortDesc())
                .type(ApplicationCommand.Type.CHAT_INPUT.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("roll")
                        .description(ROLL.getParamDescs().get("roll"))
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .build();

        RestClient restClient = client.getClient().getRestClient();

        long applicationId = restClient.getApplicationId().block();

        restClient.getApplicationService()
                .createGlobalApplicationCommand(applicationId, commandRequest)
                .doOnError(e -> log.error("Unable to create guild command", e))
                .onErrorResume(_ -> Mono.empty())
                .block();
    }
}
