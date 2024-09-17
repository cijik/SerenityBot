package com.ciji.serenity.service;

import org.springframework.stereotype.Service;

import com.ciji.serenity.enums.Commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.ciji.serenity.enums.Commands.*;

@Service
public class CommandInfoService {
    
    public Mono<Message> getHelp(ChatInputInteractionEvent event) {
        String command = getParameterValue(event, "command");
        event.deferReply().withEphemeral(true).block();

        if (command.isBlank()) {
            StringBuilder response = new StringBuilder();
            response
                .append("**Character database commands**:\n")
                .append("`/").append(ADD_CHARACTER.getCommand()).append("`: ").append(ADD_CHARACTER.getShortDesc()).append("\n")
                .append("`/").append(GET_CHARACTER.getCommand()).append("`: ").append(GET_CHARACTER.getShortDesc()).append("\n")
                .append("`/").append(GET_ALL_CHARACTERS.getCommand()).append("`: ").append(GET_ALL_CHARACTERS.getShortDesc()).append("\n")
                .append("`/").append(REMOVE_CHARACTER.getCommand()).append("`: ").append(REMOVE_CHARACTER.getShortDesc()).append("\n")
                .append("**Sheet interaction commands**:\n")
                .append("`/").append(READ_SHEET.getCommand()).append("`: ").append(READ_SHEET.getShortDesc()).append("\n")
                .append("`/").append(ROLL_TARGETED.getCommand()).append("`: ").append(ROLL_TARGETED.getShortDesc()).append("\n")
                .append("`/").append(ROLL_UNTARGETED.getCommand()).append("`: ").append(ROLL_UNTARGETED.getShortDesc()).append("\n")
                .append("**Generic roll commands**:\n")
                .append("`/").append(ROLL.getCommand()).append("`, `/").append(SHORT_ROLL.getCommand()).append("`: ").append(ROLL.getShortDesc()).append("\n")
                .append("**Help commands**:\n")
                .append("`/").append(HELP.getCommand()).append("`: ").append(HELP.getShortDesc()).append("\n")
                .append("`/").append(DOCS.getCommand()).append("`: ").append(DOCS.getShortDesc()).append("\n");
            return event.createFollowup(response.toString());
        } else {
            switch (Commands.fromString(command)) {
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
                default -> {
                    return event.createFollowup("No documentation found for command `" + command + "` or command does not exist");
                }
            }
        }
    }

    public Mono<Message> getDocs(ChatInputInteractionEvent event) {
        event.deferReply().withEphemeral(true).block();

        Button button = Button.link("https://github.com/cijik/SerenityBot/wiki", "Documentation");
            return event.createFollowup(InteractionFollowupCreateSpec.builder()
                            .components(List.of(ActionRow.of(button)))
                            .build());
    }

    private static String getParameterValue(ChatInputInteractionEvent event, String name) {
        return event
                .getOption(name)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse("");
    }

    private static Mono<Message> constructHelpResponse(ChatInputInteractionEvent event, Commands command) {
        StringBuilder response = new StringBuilder();
        response.append("`/").append(command.getCommand()).append("`:\n")
                .append(command.getFullDesc()).append("\n");
        command.getParamDescs().forEach((param, desc) -> response.append(" - `").append(param).append("`: ").append(desc).append("\n"));
        return event.createFollowup(response.toString());
    }
}
