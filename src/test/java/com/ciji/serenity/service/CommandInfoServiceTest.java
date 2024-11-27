package com.ciji.serenity.service;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.component.MessageComponent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionFollowupCreateMono;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.discordjson.json.*;
import discord4j.discordjson.possible.Possible;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandInfoServiceTest {

    @Mock
    private ChatInputInteractionEvent event;

    @Mock
    private ApplicationCommandInteractionOptionData data;

    @Mock
    private GatewayDiscordClient gateway;

    @InjectMocks
    private CommandInfoService commandInfoService;

    private ApplicationCommandInteractionOption parameter;

    @BeforeEach
    void setUp() {
        gateway = mock(GatewayDiscordClient.class);
        parameter = new ApplicationCommandInteractionOption(
                gateway,
                data,
                1L,
                mock(ApplicationCommandInteractionResolvedData.class)
        );
        when(gateway.getRestClient()).thenReturn(mock(DiscordClient.class));
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    void getHelp(String parameterValue, String expectedHelp) {
        when(event.getOption(eq("command"))).thenReturn(Optional.of(parameter));
        when(data.value()).thenReturn(Possible.of(parameterValue));
        when(data.type()).thenReturn(3);
        Message messageResponse = new Message(gateway, MessageData.builder()
                .applicationId(1L)
                .id(1L)
                .channelId(200L)
                .type(ApplicationCommandOption.Type.STRING.ordinal())
                .author(UserData.builder().id(1L).username("user").discriminator("#0").build())
                .content(expectedHelp)
                .timestamp("today")
                .tts(false)
                .mentionEveryone(false)
                .pinned(false)
                .build());

        when(event.createFollowup(any(InteractionFollowupCreateSpec.class))).thenReturn(Mono.just(messageResponse));
        when(event.createFollowup(anyString())).thenReturn(InteractionFollowupCreateMono.of(event).withContent(messageResponse.getContent()));

        Mono<Message> response = commandInfoService.getHelp(event);

        StepVerifier.create(response)
                .expectNextMatches(message -> message.getContent().equals(expectedHelp))
                .verifyComplete();
    }

    @Test
    void getDocs() {
        Message messageResponse = new Message(gateway, MessageData.builder()
                .applicationId(1L)
                .id(1L)
                .channelId(200L)
                .type(ApplicationCommandOption.Type.STRING.ordinal())
                .author(UserData.builder().id(1L).username("user").discriminator("#0").build())
                .components(ComponentData.builder().type(MessageComponent.Type.BUTTON.ordinal()).build())
                .content("")
                .timestamp("today")
                .tts(false)
                .mentionEveryone(false)
                .pinned(false)
                .build());
        when(event.createFollowup(any(InteractionFollowupCreateSpec.class))).thenReturn(Mono.just(messageResponse));

        Mono<Message> response = commandInfoService.getDocs(event);

        StepVerifier.create(response)
                .expectNextMatches(message ->
                        message.getData().components().toOptional().get().stream().anyMatch(component -> component.type() == MessageComponent.Type.BUTTON.ordinal()))
                .verifyComplete();
    }

    private static Stream<Arguments> testArguments() {
        return Stream.of(
                arguments("", "Full list of commands"),
                arguments("add-character", "Specific command description")
        );
    }
}