package com.ciji.serenity.service;

import com.ciji.serenity.model.CharacterSheet;
import com.ciji.serenity.repository.CharacterSheetRepository;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterSheetServiceTest {

    @Mock
    private ChatInputInteractionEvent event;

    @Mock
    private ApplicationCommandInteractionOptionData data;

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
        when(event.getInteraction()).thenReturn(interaction);
        when(interaction.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(snowflake);
        when(snowflake.asString()).thenReturn("1");
    }

    @Test
    void getCharacter() {
        when(event.getOption(eq("name"))).thenReturn(Optional.of(parameter));
        when(data.value()).thenReturn(Possible.of("Character"));
        when(data.type()).thenReturn(3);
        when(characterSheetRepository.findByNameAndOwnerId("Character", "1")).thenReturn(characterSheet);
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
        Message messageResponse = new Message(gateway, MessageData.builder()
                .applicationId(1L)
                .id(1L)
                .channelId(200L)
                .type(ApplicationCommandOption.Type.STRING.ordinal())
                .author(UserData.builder().id(1L).username("user").discriminator("#0").build())
                .components(ComponentData.builder().type(MessageComponent.Type.BUTTON.ordinal()).build())
                .content("Here's the list of all characters you own:\n**Character**")
                .timestamp("today")
                .tts(false)
                .mentionEveryone(false)
                .pinned(false)
                .build());
        when(event.createFollowup(any(InteractionFollowupCreateSpec.class)))
                .thenReturn(Mono.just(messageResponse));
        when(characterSheetRepository.findAllByOwnerId("1")).thenReturn(List.of(characterSheet));
        when(event.createFollowup(anyString())).thenReturn(InteractionFollowupCreateMono.of(event).withContent(messageResponse.getContent()));
        Mono<Message> response = characterSheetService.getAllCharacters(event);

        StepVerifier.create(response)
                .expectNextMatches(message -> message.getContent().equals(messageResponse.getContent()))
                .verifyComplete();
    }

    @Test
    void addCharacter() {
        ApplicationCommandInteractionOptionData data2 = mock(ApplicationCommandInteractionOptionData.class);
        ApplicationCommandInteractionOption parameter2 = new ApplicationCommandInteractionOption(
                gateway,
                data2,
                1L,
                mock(ApplicationCommandInteractionResolvedData.class)
        );
        when(event.getOption(eq("name"))).thenReturn(Optional.of(parameter));
        when(event.getOption(eq("url"))).thenReturn(Optional.of(parameter2));
        when(data.value()).thenReturn(Possible.of("Character"));
        when(data.type()).thenReturn(3);
        when(data2.value()).thenReturn(Possible.of("url0/url1/url2/url3/url4/url5"));
        when(data2.type()).thenReturn(3);

        Message messageResponse = new Message(gateway, MessageData.builder()
                .applicationId(1L)
                .id(1L)
                .channelId(200L)
                .type(ApplicationCommandOption.Type.STRING.ordinal())
                .author(UserData.builder().id(1L).username("user").discriminator("#0").build())
                .components(ComponentData.builder().type(MessageComponent.Type.BUTTON.ordinal()).build())
                .content("Character sheet added")
                .timestamp("today")
                .tts(false)
                .mentionEveryone(false)
                .pinned(false)
                .build());
        when(event.createFollowup(any(InteractionFollowupCreateSpec.class)))
                .thenReturn(Mono.just(messageResponse));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenReturn(characterSheet);
        when(event.createFollowup(anyString())).thenReturn(InteractionFollowupCreateMono.of(event).withContent(messageResponse.getContent()));
        Mono<Message> response = characterSheetService.addCharacter(event);

        StepVerifier.create(response)
                .expectNextMatches(message -> message.getContent().equals(messageResponse.getContent()))
                .verifyComplete();
    }

    @Test
    void updateCharacter() {
        ApplicationCommandInteractionOptionData data2 = mock(ApplicationCommandInteractionOptionData.class);
        ApplicationCommandInteractionOption parameter2 = new ApplicationCommandInteractionOption(
                gateway,
                data2,
                1L,
                mock(ApplicationCommandInteractionResolvedData.class)
        );
        when(event.getOption(eq("name"))).thenReturn(Optional.of(parameter));
        when(event.getOption(eq("owner-id"))).thenReturn(Optional.of(parameter2));
        when(data.value()).thenReturn(Possible.of("Character"));
        when(data.type()).thenReturn(3);
        when(data2.value()).thenReturn(Possible.of("1"));
        when(data2.type()).thenReturn(3);

        Message messageResponse = new Message(gateway, MessageData.builder()
                .applicationId(1L)
                .id(1L)
                .channelId(200L)
                .type(ApplicationCommandOption.Type.STRING.ordinal())
                .author(UserData.builder().id(1L).username("user").discriminator("#0").build())
                .components(ComponentData.builder().type(MessageComponent.Type.BUTTON.ordinal()).build())
                .content("Character **Character** has been updated with ownerId 1.")
                .timestamp("today")
                .tts(false)
                .mentionEveryone(false)
                .pinned(false)
                .build());
        when(event.createFollowup(any(InteractionFollowupCreateSpec.class)))
                .thenReturn(Mono.just(messageResponse));
        when(characterSheetRepository.findByName(anyString())).thenReturn(Optional.of(characterSheet));
        when(characterSheetRepository.save(any(CharacterSheet.class))).thenReturn(characterSheet);
        when(event.createFollowup(anyString())).thenReturn(InteractionFollowupCreateMono.of(event).withContent(messageResponse.getContent()));

        Mono<Message> response = characterSheetService.updateCharacter(event);

        StepVerifier.create(response)
                .expectNextMatches(message -> message.getContent().equals(messageResponse.getContent()))
                .verifyComplete();
    }

    @Test
    void removeCharacter() {
        when(event.getOption(eq("name"))).thenReturn(Optional.of(parameter));
        when(data.value()).thenReturn(Possible.of("Character"));
        when(data.type()).thenReturn(3);

        Message messageResponse = new Message(gateway, MessageData.builder()
                .applicationId(1L)
                .id(1L)
                .channelId(200L)
                .type(ApplicationCommandOption.Type.STRING.ordinal())
                .author(UserData.builder().id(1L).username("user").discriminator("#0").build())
                .components(ComponentData.builder().type(MessageComponent.Type.BUTTON.ordinal()).build())
                .content("Character sheet deleted")
                .timestamp("today")
                .tts(false)
                .mentionEveryone(false)
                .pinned(false)
                .build());
        when(event.createFollowup(any(InteractionFollowupCreateSpec.class)))
                .thenReturn(Mono.just(messageResponse));
        when(characterSheetRepository.findByNameAndOwnerId("Character", "1")).thenReturn(characterSheet);
        doNothing().when(characterSheetRepository).delete(characterSheet);
        when(event.createFollowup(anyString())).thenReturn(InteractionFollowupCreateMono.of(event).withContent(messageResponse.getContent()));

        Mono<Message> response = characterSheetService.removeCharacter(event);

        StepVerifier.create(response)
                .expectNextMatches(message -> message.getContent().equals(messageResponse.getContent()))
                .verifyComplete();
    }

    @Test
    void readSheetValue() throws GeneralSecurityException, IOException {
        ApplicationCommandInteractionOptionData data2 = mock(ApplicationCommandInteractionOptionData.class);
        ApplicationCommandInteractionOption parameter2 = new ApplicationCommandInteractionOption(
                gateway,
                data2,
                1L,
                mock(ApplicationCommandInteractionResolvedData.class)
        );
        when(event.getOption(eq("name"))).thenReturn(Optional.of(parameter));
        when(event.getOption(eq("value"))).thenReturn(Optional.of(parameter2));
        when(data.value()).thenReturn(Possible.of("Character"));
        when(data.type()).thenReturn(3);
        when(data2.value()).thenReturn(Possible.of("Spell"));
        when(data2.type()).thenReturn(3);
        BatchGetValuesResponse sheetMatrix = mock(BatchGetValuesResponse.class);
        ValueRange spells = mock(ValueRange.class);
        when(spells.getValues()).thenReturn(List.of(List.of("Spell")));
        ValueRange descriptions = mock(ValueRange.class);
        when(descriptions.getValues()).thenReturn(List.of(List.of("Description")));
        when(sheetMatrix.getValueRanges()).thenReturn(List.of(spells, descriptions));

        Message messageResponse = new Message(gateway, MessageData.builder()
                .applicationId(1L)
                .id(1L)
                .channelId(200L)
                .type(ApplicationCommandOption.Type.STRING.ordinal())
                .author(UserData.builder().id(1L).username("user").discriminator("#0").build())
                .components(ComponentData.builder().type(MessageComponent.Type.BUTTON.ordinal()).build())
                .content("Spell's **Description** is: Description")
                .timestamp("today")
                .tts(false)
                .mentionEveryone(false)
                .pinned(false)
                .build());
        when(event.createFollowup(any(InteractionFollowupCreateSpec.class)))
                .thenReturn(Mono.just(messageResponse));
        when(characterSheetRepository.findByNameAndOwnerId("Character", "1")).thenReturn(characterSheet);
        when(characterSheetDetailsService.getSpreadsheetMatrix(any(CharacterSheet.class), anyList())).thenReturn(sheetMatrix);
        when(event.createFollowup(anyString())).thenReturn(InteractionFollowupCreateMono.of(event).withContent(messageResponse.getContent()));

        Mono<Message> response = characterSheetService.readSheetValue(event);

        StepVerifier.create(response)
                .expectNextMatches(message -> message.getContent().equals(messageResponse.getContent()))
                .verifyComplete();
    }
}