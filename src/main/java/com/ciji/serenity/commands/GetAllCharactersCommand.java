package com.ciji.serenity.commands;

import discord4j.core.object.command.ApplicationCommand;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.ciji.serenity.enums.Command.GET_ALL_CHARACTERS;

@Component
@Slf4j
@AllArgsConstructor
public class GetAllCharactersCommand implements SerenityCommand {

    @Override
    public void register(long applicationId, RestClient restClient) {
        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
                .name(GET_ALL_CHARACTERS.getCommand())
                .description(GET_ALL_CHARACTERS.getShortDesc())
                .type(ApplicationCommand.Type.CHAT_INPUT.getValue())
                .build();

        restClient.getApplicationService()
                .createGlobalApplicationCommand(applicationId, commandRequest)
                .doOnSuccess(data -> log.info("{} command registered", StringUtils.capitalize(data.name())))
                .doOnError(e -> log.error("Unable to create guild command", e))
                .onErrorResume(_ -> Mono.empty())
                .block();
    }
}
