package com.ciji.serenity.commands;

import discord4j.core.object.command.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.ciji.serenity.enums.Command.SAY;

@Slf4j
@Component
public class SayCommand implements SerenityCommand{

    @Override
    public void register(long applicationId, RestClient restClient) {
        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
                .name(SAY.getCommand())
                .description(SAY.getShortDesc())
                .type(ApplicationCommand.Type.CHAT_INPUT.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("channel")
                        .description(SAY.getParamDescs().get("channel"))
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("message")
                        .description(SAY.getParamDescs().get("message"))
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .build();

        restClient.getApplicationService()
                .createGuildApplicationCommand(applicationId, 177794959854796801L, commandRequest)
                .doOnSuccess(data -> log.info("{} command registered", StringUtils.capitalize(data.name())))
                .doOnError(e -> log.error("Unable to create guild command", e))
                .onErrorResume(_ -> Mono.empty())
                .block();
    }
}
