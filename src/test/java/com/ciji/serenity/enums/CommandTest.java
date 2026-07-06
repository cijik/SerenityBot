package com.ciji.serenity.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CommandTest {

    @ParameterizedTest
    @ValueSource(strings = {"help", "HELP", "HeLp"})
    void fromString_returnsMatchingCommand_caseInsensitive(String value) {
        assertThat(Command.fromString(value)).isEqualTo(Command.HELP);
    }

    @Test
    void fromString_returnsNull_whenCommandDoesNotExist() {
        assertThat(Command.fromString("does-not-exist")).isNull();
    }

    @Test
    void shortRollSharesRollMetadata() {
        assertThat(Command.SHORT_ROLL.getCommand()).isEqualTo("r");
        assertThat(Command.SHORT_ROLL.getShortDesc()).isEqualTo(Command.ROLL.getShortDesc());
        assertThat(Command.SHORT_ROLL.getFullDesc()).isEqualTo(Command.ROLL.getFullDesc());
        assertThat(Command.SHORT_ROLL.getParamDescs()).isEqualTo(Command.ROLL.getParamDescs());
    }
}