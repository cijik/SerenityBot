package com.ciji.serenity.service;

import com.ciji.serenity.exception.OptionNotFoundException;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class MessageSendingService {

    public Mono<Message> sendMessage(ChatInputInteractionEvent event) {
        String channelId = getParameterValue(event, "channel");
        String message = getParameterValue(event, "message");

        return event.reply()
                .event().getClient()
                .getChannelById(Snowflake.of(channelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(message));
    }

    private static String getParameterValue(ChatInputInteractionEvent event, String name) {
        return event
                .getOption(name)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(OptionNotFoundException::new);
    }
}
