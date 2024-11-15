package com.ciji.serenity.service;

import com.ciji.serenity.model.CharacterSheet;
import com.ciji.serenity.repository.CharacterSheetRepository;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterSheetServiceTest {

    @Mock
    private ChatInputInteractionEvent event;

    private ApplicationCommandInteractionOption parameter;

    private CharacterSheet characterSheet;

    @Mock
    private CharacterSheetRepository characterSheetRepository;

    @Mock
    private CharacterSheetDetailsService characterSheetDetailsService;

    @Mock
    private GatewayDiscordClient gateway;

    @InjectMocks
    private CharacterSheetService characterSheetService;

    @BeforeEach
    void setUp() {
        ApplicationCommandInteractionOptionData data = mock(ApplicationCommandInteractionOptionData.class);
        gateway = mock(GatewayDiscordClient.class);
        parameter = new ApplicationCommandInteractionOption(
                gateway,
                data,
                1L,
                mock(ApplicationCommandInteractionResolvedData.class)
        );
        Interaction interaction = mock(Interaction.class);
        User user = mock(User.class);
        Snowflake snowflake = mock(Snowflake.class);
        characterSheet = new CharacterSheet();
        characterSheet.setId("1");
        characterSheet.setOwnerId("1");
        characterSheet.setName("Character");

        when(gateway.getRestClient()).thenReturn(mock(DiscordClient.class));
        when(event.getOption(eq("name"))).thenReturn(Optional.of(parameter));
        when(event.getInteraction()).thenReturn(interaction);
        when(interaction.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(snowflake);
        when(snowflake.asString()).thenReturn("1");
        when(data.value()).thenReturn(Possible.of("Character"));
        when(data.type()).thenReturn(3);
        when(characterSheetRepository.findByNameAndOwnerId("Character", "1")).thenReturn(characterSheet);
    }

    @Test
    void getCharacter() {
        Message messageResponse = new Message(gateway, MessageData.builder()
                .applicationId(1L)
                .id(1L)
                .channelId(200L)
                .type(ApplicationCommandOption.Type.STRING.ordinal())
                .author(UserData.builder().id(1L).username("user").discriminator("#0").build())
                .components(ComponentData.builder().type(MessageComponent.Type.BUTTON.ordinal()).build())
                .content("Character **Character**:")
                .timestamp("today")
                .tts(false)
                .mentionEveryone(false)
                .pinned(false)
                .build());
        when(event.createFollowup(any(InteractionFollowupCreateSpec.class)))
                .thenReturn(Mono.just(messageResponse));
        when(event.createFollowup(anyString())).thenReturn(InteractionFollowupCreateMono.of(event));
        Mono<Message> response = characterSheetService.getCharacter(event);

        StepVerifier.create(response)
                .expectNextMatches(message ->
                        message.getContent().equals("Character **Character**:") &&
                                message.getData().components().toOptional().get().stream().anyMatch(component -> component.type() == MessageComponent.Type.BUTTON.ordinal()))
                .verifyComplete();
    }

    @Test
    void getAllCharacters() {
    }

    @Test
    void addCharacter() {
    }

    @Test
    void updateCharacter() {
    }

    @Test
    void removeCharacter() {
    }

    @Test
    void readSheetValue() {
    }
}