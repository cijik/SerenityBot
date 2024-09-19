package com.ciji.serenity.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ciji.serenity.commands.SerenityCommand;
import com.ciji.serenity.config.mapper.ApplicationCommandRequestMapper;
import com.ciji.serenity.enums.Command;
import com.ciji.serenity.service.adapter.SerenityEventAdapter;

import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.rest.RestClient;
import lombok.AllArgsConstructor;

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
        if (existingCommands != null && //could be null
                (existingCommands.removeIf(existingCommand -> Command.fromString(existingCommand.name()) == null) //check for any deprecated commands
                        || !Arrays.stream(Command.values()).allMatch(command -> existingCommands.stream().anyMatch(existingCommand -> existingCommand.name().equals(command.getCommand()))))) { //check for any new commands
            restClient.getApplicationService().bulkOverwriteGlobalApplicationCommand(applicationId, existingCommands.stream().map(ApplicationCommandRequestMapper::map).toList()).subscribe();
            for (SerenityCommand command : commandList) {
                command.register();
            }
        }

        eventAdapter.updatePresenceOnCommandInit(client);
        client.getClient().on(eventAdapter).blockLast();
    }
}
