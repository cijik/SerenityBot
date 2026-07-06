package com.ciji.serenity.service;

import com.ciji.serenity.model.CharacterSheet;
import com.ciji.serenity.model.CharacterSheetDetails;
import com.ciji.serenity.model.SheetMatrix;
import com.ciji.serenity.model.SheetRow;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionFollowupCreateMono;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.discordjson.json.ComponentData;
import discord4j.discordjson.json.MemberData;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.UserData;
import discord4j.discordjson.possible.Possible;
import discord4j.common.util.Snowflake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RollProcessingServiceTest {

    @Mock
    private CharacterSheetService characterSheetService;

    @Mock
    private CharacterSheetDetailsService characterSheetDetailsService;

    @Mock
    private ChatInputInteractionEvent event;

    @Mock
    private GatewayDiscordClient gateway;

    @Mock
    private Interaction interaction;

    @Mock
    private Member member;

    @Mock
    private MemberData memberData;

    @Mock
    private User user;

    @Mock
    private Snowflake snowflake;

    @Mock
    private RollRandomSource rollRandomSource;

    @InjectMocks
    private RollProcessingService rollProcessingService;

    @BeforeEach
    void setUp() {
        when(gateway.getRestClient()).thenReturn(mock(DiscordClient.class));
        when(event.getInteraction()).thenReturn(interaction);
        when(interaction.getMember()).thenReturn(Optional.of(member));
        when(member.getMemberData()).thenReturn(memberData);
        when(memberData.nick()).thenReturn(Possible.of(Optional.of("Tester")));
        when(interaction.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(snowflake);
        when(snowflake.asString()).thenReturn("1");
        when(user.getGlobalName()).thenReturn(Optional.empty());
        when(user.getUsername()).thenReturn("tester");

        when(event.createFollowup(anyString())).thenAnswer(invocation ->
                InteractionFollowupCreateMono.of(event).withContent(invocation.getArgument(0, String.class)));
        when(event.createFollowup(any(InteractionFollowupCreateSpec.class))).thenAnswer(invocation -> {
            InteractionFollowupCreateSpec spec = invocation.getArgument(0, InteractionFollowupCreateSpec.class);
            String content = spec.content().toOptional().orElse("");
            return Mono.just(messageWithContent(content));
        });

        rollProcessingService.unrig(event);
    }

    @Test
    void rigMarksNextRollsAsRigged() {
        stubOption("type", "pass");
        stubOption("is-crit", "yes");

        Mono<Message> response = rollProcessingService.rig(event);

        assertThat(response).isInstanceOf(InteractionFollowupCreateMono.class);
        InteractionFollowupCreateMono mono = (InteractionFollowupCreateMono) response;
        assertThat(mono.content().toOptional()).contains("All next rolls are rigged");
        assertThat(mono.ephemeral().toOptional()).contains(true);
    }

    @Test
    void rigRejectsInvalidRigType() {
        stubOption("type", "banana");
        stubOption("is-crit", "no");

        Mono<Message> response = rollProcessingService.rig(event);

        assertThat(response).isInstanceOf(InteractionFollowupCreateMono.class);
        InteractionFollowupCreateMono mono = (InteractionFollowupCreateMono) response;
        assertThat(mono.content().toOptional()).contains("Invalid type of rigging. Please use pass/fail.");
        assertThat(mono.ephemeral().toOptional()).contains(true);
    }

    @Test
    void unrigClearsRiggingState() {
        stubOption("type", "pass");
        stubOption("is-crit", "yes");
        rollProcessingService.rig(event);

        Mono<Message> response = rollProcessingService.unrig(event);

        assertThat(response).isInstanceOf(InteractionFollowupCreateMono.class);
        InteractionFollowupCreateMono mono = (InteractionFollowupCreateMono) response;
        assertThat(mono.content().toOptional()).contains("All next rolls are no longer rigged.");
        assertThat(mono.ephemeral().toOptional()).contains(true);
    }

    @Test
    void rollRejectsInvalidExpressions() {
        stubOption("roll", "abc");

        Mono<Message> response = rollProcessingService.roll(event);

        assertThat(response).isInstanceOf(InteractionFollowupCreateMono.class);
        InteractionFollowupCreateMono mono = (InteractionFollowupCreateMono) response;
        assertThat(mono.content().toOptional()).contains("Invalid roll expression");
    }

    @Test
    void rollExpandsDiceAndIncludesComment() {
        stubOption("roll", "1d1+1#keep this");
        when(rollRandomSource.nextInt(1, 2)).thenReturn(1);

        StepVerifier.create(rollProcessingService.roll(event))
                .expectNextMatches(message -> message.getContent().contains("**Tester** rolls **2**")
                        && message.getContent().contains("with comment: 'keep this'")
                        && message.getContent().contains("Roll details: 1d1+1")
                        && message.getContent().contains("1d1: [ 1 ]"))
                .verifyComplete();
    }

    @Test
    void rollTargetedReturnsDeterministicResult() {
        CharacterSheet sheet = characterSheet();
        CharacterSheetDetails details = skillDetails();

        stubOption("character-name", "Character");
        stubOption("with-target-mfd", "1");
        stubOption("rolls-for", "magic");
        when(characterSheetService.getCharacterSheet("Character", "1")).thenReturn(Mono.just(sheet));
        when(characterSheetDetailsService.getCharacterSheetDetails(sheet)).thenReturn(details);
        when(rollRandomSource.nextInt(1, 101)).thenReturn(15);

        StepVerifier.create(rollProcessingService.rollTargeted(event))
                .expectNextMatches(message -> message.getContent().contains("Character rolls **15** for Magic with target MFD **1** [**20**], Success!"))
                .verifyComplete();
    }

    @Test
    void rollUntargetedReturnsDeterministicResult() {
        CharacterSheet sheet = characterSheet();
        CharacterSheetDetails details = skillDetails();

        stubOption("character-name", "Character");
        stubOption("with-step-bonus", "0");
        stubOption("rolls-for", "magic");
        when(characterSheetService.getCharacterSheet("Character", "1")).thenReturn(Mono.just(sheet));
        when(characterSheetDetailsService.getCharacterSheetDetails(sheet)).thenReturn(details);
        when(rollRandomSource.nextInt(1, 101)).thenReturn(50);

        StepVerifier.create(rollProcessingService.rollUntargeted(event))
                .expectNextMatches(message -> message.getContent().contains("Character rolls **50** for Magic succeeding MFD **1 1/2** (**60**)"))
                .verifyComplete();
    }

    private void stubOption(String name, String value) {
        ApplicationCommandInteractionOption option = mock(ApplicationCommandInteractionOption.class);
        ApplicationCommandInteractionOptionValue optionValue = mock(ApplicationCommandInteractionOptionValue.class);

        when(event.getOption(name)).thenReturn(Optional.of(option));
        when(option.getValue()).thenReturn(Optional.of(optionValue));
        when(optionValue.asString()).thenReturn(value);
    }

    private CharacterSheet characterSheet() {
        CharacterSheet sheet = new CharacterSheet();
        sheet.setId("sheet-1");
        sheet.setName("Character");
        sheet.setOwnerId("1");
        return sheet;
    }

    private CharacterSheetDetails skillDetails() {
        SheetRow row = new SheetRow();
        row.setRow(java.util.List.of("100", "80", "60", "40", "20", "10", "5"));

        SheetMatrix matrix = new SheetMatrix();
        matrix.setHeaders(java.util.List.of("Magic"));
        matrix.setRows(java.util.List.of(row));

        CharacterSheetDetails details = new CharacterSheetDetails();
        details.setName("Character");
        details.setSkillMatrix(matrix);
        details.setSpecialsMatrix(matrix);
        return details;
    }

    private Message messageWithContent(String content) {
        return new Message(gateway, MessageData.builder()
                .applicationId(1L)
                .id(1L)
                .channelId(200L)
                .type(0)
                .author(UserData.builder().id(1L).username("user").discriminator("#0").build())
                .components(ComponentData.builder().type(0).build())
                .content(content)
                .timestamp("today")
                .tts(false)
                .mentionEveryone(false)
                .pinned(false)
                .build());
    }
}
