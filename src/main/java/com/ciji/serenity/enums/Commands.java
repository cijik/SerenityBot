package com.ciji.serenity.enums;

import lombok.Getter;

@Getter
public enum Commands {

    TODO("todo"),

    GET_CHARACTER("get-character"),

    ADD_CHARACTER("add-character"),

    REMOVE_CHARACTER("remove-character"),

    READ_SHEET("read-sheet"),

    ROLL_ATTRIBUTE("roll-attribute"),

    ROLL_MFD("roll-mfd"),

    ROLL("roll"),

    SHORT_ROLL("r");

    private final String command;

    Commands(String command) {
        this.command = command;
    }

    public static Commands fromString(String value) {
        for (Commands c : Commands.values()) {
            if (c.command.equalsIgnoreCase(value)) {
                return c;
            }
        }
        throw new IllegalArgumentException("No constant with text " + value + " found");
    }
}
