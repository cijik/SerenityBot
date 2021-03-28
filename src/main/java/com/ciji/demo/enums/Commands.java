package com.ciji.demo.enums;

public enum Commands {

    TODO("todo"),

    GET_TIMER("getTimer"),

    START_TIMER("startTimer"),

    STOP_TIMER("stopTimer"),

    SET_PREFIX("setPrefix"),

    GET_DATE("getDate"),

    SET_DATE("setDate"),

    ADD_DAYS("addDays");

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
