package com.ciji.serenity.service;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SheetsUtilTest {

    @Test
    void getParameterValue_returnsEmptyString_whenOptionMissing() {
        ChatInputInteractionEvent event = mock(ChatInputInteractionEvent.class);
        when(event.getOption("name")).thenReturn(Optional.empty());

        assertThat(SheetsUtil.getParameterValue(event, "name")).isEqualTo("");
    }

    @Test
    void getParameterValue_returnsStringValue_whenOptionPresent() {
        ChatInputInteractionEvent event = mock(ChatInputInteractionEvent.class);
        ApplicationCommandInteractionOption option = mock(ApplicationCommandInteractionOption.class);
        ApplicationCommandInteractionOptionValue value = mock(ApplicationCommandInteractionOptionValue.class);

        when(event.getOption("name")).thenReturn(Optional.of(option));
        when(option.getValue()).thenReturn(Optional.of(value));
        when(value.asString()).thenReturn("Character");

        assertThat(SheetsUtil.getParameterValue(event, "name")).isEqualTo("Character");
    }
}