package com.ciji.serenity.commands;

import discord4j.core.object.command.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.ciji.serenity.enums.Command.ROLL_TARGETED;

@Component
@Slf4j
@AllArgsConstructor
public class RollTargetedCommand implements SerenityCommand {

    @Override
    public void register(long applicationId, RestClient restClient) {
        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
                .name(ROLL_TARGETED.getCommand())
                .description(ROLL_TARGETED.getShortDesc())
                .type(ApplicationCommand.Type.CHAT_INPUT.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("character-name")
                        .description(ROLL_TARGETED.getParamDescs().get("character-name"))
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("rolls-for")
                        .description(ROLL_TARGETED.getParamDescs().get("rolls-for"))
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("with-target-mfd")
                        .description(ROLL_TARGETED.getParamDescs().get("with-target-mfd"))
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .build();

        restClient.getApplicationService()
                .createGlobalApplicationCommand(applicationId, commandRequest)
                .doOnSuccess(data -> log.info("{} command registered", StringUtils.capitalize(data.name())))
                .doOnError(e -> log.error("Unable to create guild command", e))
                .onErrorResume(_ -> Mono.empty())
                .block();
    }
}
