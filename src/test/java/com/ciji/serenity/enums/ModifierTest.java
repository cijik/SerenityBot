package com.ciji.serenity.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModifierTest {

    @ParameterizedTest
    @ValueSource(strings = {"1", "1/2", "1 1/2", "3/4"})
    void modifierFromStringParsesKnownValues(String value) {
        assertThat(Modifier.fromString(value).getModifier()).isEqualTo(value);
    }

    @Test
    void modifierFromStringThrowsForUnknownValue() {
        assertThatThrownBy(() -> Modifier.fromString("bad-value"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}