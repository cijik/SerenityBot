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

    @ParameterizedTest
    @ValueSource(strings = {"get-inventory", "GET-INVENTORY", "Get-Inventory"})
    void fromString_returnsGetInventory_caseInsensitive(String value) {
        assertThat(Command.fromString(value)).isEqualTo(Command.GET_INVENTORY);
    }

    @Test
    void getInventoryMetadata() {
        assertThat(Command.GET_INVENTORY.getCommand()).isEqualTo("get-inventory");
        assertThat(Command.GET_INVENTORY.getShortDesc()).isEqualTo("Get a character's inventory");
        assertThat(Command.GET_INVENTORY.getFullDesc()).isEqualTo("Get a character's inventory, both main and extra, listing all items, their weights, amount and value");
        assertThat(Command.GET_INVENTORY.getParamDescs()).containsEntry("character-name", "Name of the character to get the inventory for");
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