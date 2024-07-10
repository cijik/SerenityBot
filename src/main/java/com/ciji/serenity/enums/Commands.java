package com.ciji.serenity.enums;

public enum Commands {

    TODO("todo"),

    GET_CHARACTER("get-character"),

    ADD_CHARACTER("add-character"),

    REMOVE_CHARACTER("remove-character"),

    READ_SHEET("read-sheet"),

    ROLL_SKILL("roll-skill");

    private String command;

    Commands(String command) {
        this.command = command;
    }

    public String getCommand() {
        return this.command;
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
