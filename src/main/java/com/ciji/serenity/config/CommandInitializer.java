package com.ciji.serenity.config;

import com.ciji.serenity.commands.SerenityCommand;
import com.ciji.serenity.service.SerenityEventAdapter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class CommandInitializer {

    private final Client client;

    private final List<SerenityCommand> commandList;

    private final SerenityEventAdapter eventAdapter;

    public void initializeCommands() {
        for (SerenityCommand command : commandList) {
            command.register();
        }
        client.getClient().on(eventAdapter).blockLast();
    }
}
