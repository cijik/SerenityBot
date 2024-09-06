package com.ciji.serenity.config;

import com.ciji.serenity.commands.SerenityCommand;
import com.ciji.serenity.config.mapper.ApplicationCommandRequestMapper;
import com.ciji.serenity.enums.Commands;
import com.ciji.serenity.service.adapter.SerenityEventAdapter;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.rest.RestClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
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
        if (existingCommands != null && existingCommands.removeIf(existingCommand -> Commands.fromString(existingCommand.name()) == null)) {
            restClient.getApplicationService().bulkOverwriteGlobalApplicationCommand(applicationId, existingCommands.stream().map(ApplicationCommandRequestMapper::map).toList()).subscribe();
            for (SerenityCommand command : commandList) {
                command.register();
            }
        }

        eventAdapter.updatePresenceOnCommandInit(client);
        client.getClient().on(eventAdapter).blockLast();
    }
}
