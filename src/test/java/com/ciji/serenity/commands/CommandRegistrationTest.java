package com.ciji.serenity.commands;

import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandRegistrationTest {

    @Mock
    private RestClient restClient;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private ApplicationCommandData commandData;

    @BeforeEach
    void setUp() {
        when(restClient.getApplicationService()).thenReturn(applicationService);
        when(commandData.name()).thenReturn("registered-command");
        when(applicationService.createGlobalApplicationCommand(eq(123L), any(ApplicationCommandRequest.class)))
                .thenReturn(Mono.just(commandData));
    }

    @Test
    void helpCommandRegistersWithOptionalParameter() {
        new HelpCommand().register(123L, restClient);

        assertRequest("help", "Displays full list of commands, or one command in detail", 1,
                request -> {
                    assertThat(request.options().toOptional()).contains(List.of(option("command", false, "Specific command to get info about")));
                    assertThat(request.defaultMemberPermissions()).isEmpty();
                });
    }

    @Test
    void rollCommandRegistersRequiredRollParameter() {
        new RollCommand().register(123L, restClient);

        assertRequest("roll", "Rolls one or several dice", 1,
                request -> assertThat(request.options().toOptional()).contains(List.of(option("roll", true, "The roll expression"))));
    }

    @Test
    void docsCommandRegistersWithoutOptions() {
        new DocsCommand().register(123L, restClient);

        assertRequest("docs", "Gives a link to the documentation", 1,
                request -> assertThat(request.options().isAbsent()).isTrue());
    }

    @Test
    void rigCommandRegistersWithAdminPermissionAndTwoOptions() {
        new RigCommand().register(123L, restClient);

        assertRequest("rig", "Rig the upcoming roll", 1,
                request -> {
                    assertThat(request.defaultMemberPermissions()).contains(String.valueOf(PermissionSet.of(Permission.ADMINISTRATOR).getRawValue()));
                    assertThat(request.options().toOptional()).contains(List.of(
                            option("type", true, "Type of roll: pass/fail"),
                            option("is-crit", true, "Whether the rigged roll should be critical or not: yes/no")
                    ));
                });
    }

    @Test
    void unrigCommandRegistersWithAdminPermission() {
        new UnrigCommand().register(123L, restClient);

        assertRequest("unrig", "Unrig the upcoming roll", 1,
                request -> assertThat(request.defaultMemberPermissions()).contains(String.valueOf(PermissionSet.of(Permission.ADMINISTRATOR).getRawValue())));
    }

    @Test
    void shortRollCommandRegistersAliasWithRequiredParameter() {
        new ShortRollCommand().register(123L, restClient);

        assertRequest("r", "Rolls one or several dice", 1,
                request -> assertThat(request.options().toOptional()).contains(List.of(option("roll", true, "The roll expression"))));
    }

    private void assertRequest(String name, String description, int type, java.util.function.Consumer<ApplicationCommandRequest> assertions) {
        ArgumentCaptor<ApplicationCommandRequest> captor = ArgumentCaptor.forClass(ApplicationCommandRequest.class);

        verify(applicationService).createGlobalApplicationCommand(eq(123L), captor.capture());
        ApplicationCommandRequest request = captor.getValue();

        assertThat(request.name()).isEqualTo(name);
        assertThat(request.description()).isEqualTo(Possible.of(description));
        assertThat(request.type()).isEqualTo(Possible.of(type));
        assertions.accept(request);
    }

    private static ApplicationCommandOptionData option(String name, boolean required, String description) {
        return ApplicationCommandOptionData.builder()
                .name(name)
                .description(description)
                .type(3)
                .required(required)
                .build();
    }
}
