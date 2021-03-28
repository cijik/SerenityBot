package com.ciji.serenity.listeners;

import org.springframework.stereotype.Service;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

@Service
public class MessageCreateListener extends MessageListener implements EventListener<MessageCreateEvent> {

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        if (event.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false)) {
            return processCommand(event.getMessage());
        } else {
            return Mono.empty();
        }
    }
}
