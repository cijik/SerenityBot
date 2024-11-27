package com.ciji.serenity.service;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class MessageSendingService {

    public Mono<Message> sendMessage(ChatInputInteractionEvent event) {
        String channelId = SheetsUtil.getParameterValue(event, "channel");
        String message = SheetsUtil.getParameterValue(event, "message");

        return event.reply()
                .event().getClient()
                .getChannelById(Snowflake.of(channelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(message));
    }

}
