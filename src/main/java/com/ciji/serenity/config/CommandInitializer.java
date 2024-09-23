package com.ciji.serenity.config;

import com.ciji.serenity.commands.SerenityCommand;
import com.ciji.serenity.enums.Command;
import com.ciji.serenity.service.adapter.SerenityEventAdapter;
import com.google.common.base.CaseFormat;
import discord4j.rest.RestClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@AllArgsConstructor
public class CommandInitializer {

    private final Client client;

    private final List<SerenityCommand> commandList;

    private final SerenityEventAdapter eventAdapter;

    public void initializeCommands() {
        RestClient restClient = client.getClient().getRestClient();

        restClient.getApplicationId().subscribe(applicationId ->
                client.getClient().getRestClient().getApplicationService().getGlobalApplicationCommands(applicationId).collectList().subscribe(existingCommands -> {
                    existingCommands.stream().dropWhile(existingCommand -> Command.fromString(existingCommand.name()) != null).forEach(
                            removedCommand -> restClient.getApplicationService().deleteGlobalApplicationCommand(applicationId, removedCommand.id().asLong()).subscribe()
                    );
                    Arrays.stream(Command.values()).dropWhile(command -> existingCommands.stream().anyMatch(existingCommand -> command.getCommand().equals(existingCommand.name()))).forEach(
                            addedCommand -> commandList.stream()
                                    .takeWhile(serenityCommand -> CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, serenityCommand.getClass().getName()).equals(addedCommand.getCommand()))
                                    .forEach(command -> command.register(applicationId, restClient))
                    );
                })
        );

        eventAdapter.updatePresenceOnCommandInit(client);
        client.getClient().on(eventAdapter).blockLast();
    }
}
