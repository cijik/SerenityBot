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
                return event.deferReply().withEphemeral(true).then(characterSheetService.getCharacter(event));
            }
            case GET_ALL_CHARACTERS -> {
                return event.deferReply().withEphemeral(true).then(characterSheetService.getAllCharacters(event));
            }
            case ADD_CHARACTER -> {
                return event.deferReply().withEphemeral(true).then(characterSheetService.addCharacter(event));
            }
            case UPDATE_CHARACTER -> {
                return event.deferReply().withEphemeral(true).then(characterSheetService.updateCharacter(event));
            }
            case REMOVE_CHARACTER -> {
                return event.deferReply().withEphemeral(true).then(characterSheetService.removeCharacter(event));
            }
            case READ_SHEET -> {
                return event.deferReply().withEphemeral(true).then(characterSheetService.readSheetValue(event));
            }
            case ROLL_TARGETED -> {
                return event.deferReply().then(rollProcessingService.rollTargeted(event));
            }
            case ROLL_UNTARGETED -> {
                return event.deferReply().then(rollProcessingService.rollUntargeted(event));
            }
            case ROLL, SHORT_ROLL -> {
                return event.deferReply().then(rollProcessingService.roll(event));
            }
            case HELP -> {
                return event.deferReply().withEphemeral(true).then(commandInfoService.getHelp(event));
            }
            case DOCS -> {
                return event.deferReply().withEphemeral(true).then(commandInfoService.getDocs(event));
            }
            case SAY -> {
                return messageSendingService.sendMessage(event);
            }
        }
        return Mono.empty();
    }

    public void updatePresenceOnCommandInit(Client client) {
        client.getClient().updatePresence(ClientPresence.of(Status.ONLINE, ClientActivity.listening("requests"))).subscribe();
//        client.getClient().updatePresence(ClientPresence.of(Status.DO_NOT_DISTURB, ClientActivity.custom("Debugging, do not interact"))).subscribe();
    }
}
