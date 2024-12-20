package com.ciji.serenity.service;

import org.springframework.stereotype.Service;

import com.ciji.serenity.enums.Command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.ciji.serenity.enums.Command.*;

@Service
public class CommandInfoService {
    
    public Mono<Message> getHelp(ChatInputInteractionEvent event) {
        String command = SheetsUtil.getParameterValue(event, "command");

        if (command.isBlank()) {
            String response = "**Character database commands**:\n" +
                    "`/" + ADD_CHARACTER.getCommand() + "`: " + ADD_CHARACTER.getShortDesc() + "\n" +
                    "`/" + GET_CHARACTER.getCommand() + "`: " + GET_CHARACTER.getShortDesc() + "\n" +
                    "`/" + GET_ALL_CHARACTERS.getCommand() + "`: " + GET_ALL_CHARACTERS.getShortDesc() + "\n" +
                    "`/" + REMOVE_CHARACTER.getCommand() + "`: " + REMOVE_CHARACTER.getShortDesc() + "\n" +
                    "**Sheet interaction commands**:\n" +
                    "`/" + READ_SHEET.getCommand() + "`: " + READ_SHEET.getShortDesc() + "\n" +
                    "`/" + ROLL_TARGETED.getCommand() + "`: " + ROLL_TARGETED.getShortDesc() + "\n" +
                    "`/" + ROLL_UNTARGETED.getCommand() + "`: " + ROLL_UNTARGETED.getShortDesc() + "\n" +
                    "`/" + SET_RADIATION.getCommand() + "`: " + SET_RADIATION.getShortDesc() + "\n" +
                    "`/" + SET_TEMPERATURE.getCommand() + "`: " + SET_TEMPERATURE.getShortDesc() + "\n" +
                    "**Generic roll commands**:\n" +
                    "`/" + ROLL.getCommand() + "`, `/" + SHORT_ROLL.getCommand() + "`: " + ROLL.getShortDesc() + "\n" +
                    "**Help commands**:\n" +
                    "`/" + HELP.getCommand() + "`: " + HELP.getShortDesc() + "\n" +
                    "`/" + DOCS.getCommand() + "`: " + DOCS.getShortDesc() + "\n";
            return event.createFollowup(response);
        } else {
            switch (Command.fromString(command)) {
                case ADD_CHARACTER -> {
                    return constructHelpResponse(event, ADD_CHARACTER);
                }
                case GET_CHARACTER -> {
                    return constructHelpResponse(event, GET_CHARACTER);
                }
                case GET_ALL_CHARACTERS -> {
                    return constructHelpResponse(event, GET_ALL_CHARACTERS);
                }
                case REMOVE_CHARACTER -> {
                    return constructHelpResponse(event, REMOVE_CHARACTER);
                }
                case READ_SHEET -> {
                    return constructHelpResponse(event, READ_SHEET);
                }
                case ROLL_TARGETED -> {
                    return constructHelpResponse(event, ROLL_TARGETED);
                }
                case ROLL_UNTARGETED -> {
                    return constructHelpResponse(event, ROLL_UNTARGETED);
                }
                case ROLL, SHORT_ROLL -> {
                    return constructHelpResponse(event, ROLL);
                }
                case HELP -> {
                    return constructHelpResponse(event, HELP);
                }
                case DOCS -> {
                    return constructHelpResponse(event, DOCS);
                }
                case SET_RADIATION -> {
                    return constructHelpResponse(event, SET_RADIATION);
                }
                case SET_TEMPERATURE -> {
                    return constructHelpResponse(event, SET_TEMPERATURE);
                }
                default -> {
                    return event.createFollowup("No documentation found for command `" + command + "` or command does not exist");
                }
            }
        }
    }

    public Mono<Message> getDocs(ChatInputInteractionEvent event) {
        Button button = Button.link("https://github.com/cijik/SerenityBot/wiki", "Documentation");
        return event.createFollowup(InteractionFollowupCreateSpec.builder()
                        .components(List.of(ActionRow.of(button)))
                        .build());
    }

    private static Mono<Message> constructHelpResponse(ChatInputInteractionEvent event, Command command) {
        StringBuilder response = new StringBuilder();
        response.append("`/").append(command.getCommand()).append("`:\n")
                .append(command.getFullDesc()).append("\n");
        command.getParamDescs().forEach((param, desc) -> response.append(" - `").append(param).append("`: ").append(desc).append("\n"));
        return event.createFollowup(response.toString());
    }
}
