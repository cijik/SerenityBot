package com.ciji.serenity.config.mapper;

import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.possible.Possible;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApplicationCommandRequestMapperTest {

    @Test
    void mapsCommandDataToRequest() {
        ApplicationCommandData commandData = mock(ApplicationCommandData.class);
        when(commandData.name()).thenReturn("roll");
        when(commandData.description()).thenReturn("Roll dice");
        when(commandData.type()).thenReturn(Possible.of(1));

        ApplicationCommandRequest request = ApplicationCommandRequestMapper.map(commandData);

        assertThat(request.name()).isEqualTo("roll");
        assertThat(request.description()).isEqualTo(Possible.of("Roll dice"));
        assertThat(request.type()).isEqualTo(Possible.of(1));
    }
}
