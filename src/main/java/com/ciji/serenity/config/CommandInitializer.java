package com.ciji.serenity.config;

import com.ciji.serenity.commands.SerenityCommand;
import com.ciji.serenity.config.mappers.ApplicationCommandRequestMapper;
import com.ciji.serenity.enums.Commands;
import com.ciji.serenity.service.SerenityEventAdapter;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.rest.RestClient;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class CommandInitializer {

    private final Client client;

    private final List<SerenityCommand> commandList;

    private final SerenityEventAdapter eventAdapter;

    public void initializeCommands() {
        RestClient restClient = client.getClient().getRestClient();

        long applicationId = restClient.getApplicationId().block();

        List<ApplicationCommandData> existingCommands = client.getClient().getRestClient().getApplicationService().getGlobalApplicationCommands(applicationId).collectList().block();
        if (existingCommands != null) {
            if (existingCommands.removeIf(existingCommand -> Commands.fromString(existingCommand.name()) == null)) {
                restClient.getApplicationService().bulkOverwriteGlobalApplicationCommand(applicationId, existingCommands.stream().map(ApplicationCommandRequestMapper::map).toList()).subscribe();
            }
        }

        for (SerenityCommand command : commandList) {
            command.register();
        }
        client.getClient().on(eventAdapter).blockLast();
    }
}
