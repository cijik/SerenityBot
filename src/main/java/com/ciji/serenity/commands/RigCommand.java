package com.ciji.serenity.commands;

import discord4j.core.object.command.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.ciji.serenity.enums.Command.RIG;

@Component
@Slf4j
@AllArgsConstructor
public class RigCommand implements SerenityCommand{

    @Override
    public void register(long applicationId, RestClient restClient) {
        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
                .name(RIG.getCommand())
                .description(RIG.getShortDesc())
                .type(ApplicationCommand.Type.CHAT_INPUT.getValue())
                .defaultMemberPermissions(String.valueOf(PermissionSet.of(Permission.ADMINISTRATOR)))
                .addOption(ApplicationCommandOptionData.builder()
                        .name("type")
                        .description(RIG.getParamDescs().get("type"))
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("is-crit")
                        .description(RIG.getParamDescs().get("is-crit"))
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .build();

        restClient.getApplicationService()
                .createGlobalApplicationCommand(applicationId, commandRequest)
                .doOnSuccess(data -> log.info("{} command registered", StringUtils.capitalize(data.name())))
                .doOnError(e -> log.error("Unable to create guild command", e))
                .onErrorResume(e -> Mono.empty())
                .block();
    }
}
