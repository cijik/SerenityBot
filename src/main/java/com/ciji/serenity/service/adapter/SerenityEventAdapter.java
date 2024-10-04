package com.ciji.serenity.service.adapter;

import com.ciji.serenity.config.Client;
import com.ciji.serenity.enums.Command;
import com.ciji.serenity.service.CharacterSheetService;
import com.ciji.serenity.service.CommandInfoService;
import com.ciji.serenity.service.MessageSendingService;
import com.ciji.serenity.service.RollProcessingService;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import lombok.AllArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class SerenityEventAdapter extends ReactiveEventAdapter {

    private final CharacterSheetService characterSheetService;

    private final RollProcessingService rollProcessingService;

    private final CommandInfoService commandInfoService;

    private final MessageSendingService messageSendingService;

    @Override
    public Publisher<?> onChatInputInteraction(ChatInputInteractionEvent event) {
        switch (Command.fromString(event.getCommandName())) {
            case GET_CHARACTER -> {
                return characterSheetService.getCharacter(event);
            }
            case GET_ALL_CHARACTERS -> {
                return characterSheetService.getAllCharacters(event);
            }
            case ADD_CHARACTER -> {
                return characterSheetService.addCharacter(event);
            }
            case UPDATE_CHARACTER -> {
                return characterSheetService.updateCharacter(event);
            }
            case REMOVE_CHARACTER -> {
                return characterSheetService.removeCharacter(event);
            }
            case READ_SHEET -> {
                return characterSheetService.readSheetValue(event);
            }
            case ROLL_TARGETED -> {
                return rollProcessingService.rollTargeted(event);
            }
            case ROLL_UNTARGETED -> {
                return rollProcessingService.rollUntargeted(event);
            }
            case ROLL, SHORT_ROLL -> {
                return rollProcessingService.roll(event);
            }
            case HELP -> {
                return commandInfoService.getHelp(event);
            }
            case DOCS -> {
                return commandInfoService.getDocs(event);
            }
            case SAY -> {
                return messageSendingService.sendMessage(event);
            }
        }
        return Mono.empty();
    }

    public void updatePresenceOnCommandInit(Client client) {
//        client.getClient().updatePresence(ClientPresence.of(Status.ONLINE, ClientActivity.listening("requests"))).subscribe();
        client.getClient().updatePresence(ClientPresence.of(Status.DO_NOT_DISTURB, ClientActivity.custom("Debugging, do not interact"))).subscribe();
    }
}
