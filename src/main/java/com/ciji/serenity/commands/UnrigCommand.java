package com.ciji.serenity.commands;

import discord4j.core.object.command.ApplicationCommand;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.ciji.serenity.enums.Command.UNRIG;

@Component
@Slf4j
@AllArgsConstructor
public class UnrigCommand implements SerenityCommand {

    @Override
    public void register(long applicationId, RestClient restClient) {
        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
                .name(UNRIG.getCommand())
                .description(UNRIG.getShortDesc())
                .type(ApplicationCommand.Type.CHAT_INPUT.getValue())
                .defaultMemberPermissions(String.valueOf(PermissionSet.of(Permission.ADMINISTRATOR)))
                .build();

        restClient.getApplicationService()
                .createGlobalApplicationCommand(applicationId, commandRequest)
                .doOnSuccess(data -> log.info("{} command registered", StringUtils.capitalize(data.name())))
                .doOnError(e -> log.error("Unable to create guild command", e))
                .onErrorResume(e -> Mono.empty())
                .block();
    }
}
