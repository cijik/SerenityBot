package com.ciji.serenity.service;

import com.ciji.serenity.config.Client;
import com.ciji.serenity.enums.Commands;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.MessageData;
import lombok.AllArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class SerenityEventAdapter extends ReactiveEventAdapter {

    private final CharacterSheetService characterSheetService;

    @Override
    public Publisher<?> onApplicationCommandInteraction(ApplicationCommandInteractionEvent event) {
        switch (Commands.fromString(event.getCommandName())) {
            case TODO -> {
                return doTodo(event);
            }
        }
        return Mono.empty();
    }

    @Override
    public Publisher<?> onChatInputInteraction(ChatInputInteractionEvent event) {
        switch (Commands.fromString(event.getCommandName())) {
            case GET_CHARACTER -> {
                return characterSheetService.getCharacter(event);
            }
            case ADD_CHARACTER -> {
                return characterSheetService.addCharacter(event);
            }
            case REMOVE_CHARACTER -> {
                return characterSheetService.removeCharacter(event);
            }
            case READ_SHEET -> {
                return characterSheetService.readSheetValue(event);
            }
            case ROLL_SKILL -> {
                return characterSheetService.rollSkill(event);
            }
            case ROLL_SPECIAL -> {
                return characterSheetService.rollSpecial(event);
            }
            case ROLL_MFD -> {
                return characterSheetService.rollMFD(event);
            }
        }
        return Mono.empty();
    }

    private Mono<MessageData> doTodo(ApplicationCommandInteractionEvent event) {
        return event.reply()
                .then(event.getInteractionResponse()
                        .createFollowupMessage("Things to do today:\n - write a bot\n - eat lunch\n - play a game"));
    }
}
