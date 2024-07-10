package com.ciji.serenity.enums;

public enum Modifiers {
    TWO("2"),
    ONE_AND_ONE_HALF("1 1/2"),
    ONE("1"),
    THREE_QUARTERS("3/4"),
    ONE_HALF("1/2"),
    ONE_QUARTER("1/4"),
    ONE_TENTH("1/10");

    private String modifier;

    Modifiers(String modifier) {
        this.modifier = modifier;
    }

    public String getModifier() {
        return modifier;
    }

    public static Modifiers fromString(String value) {
        for (Modifiers m : Modifiers.values()) {
            if (m.modifier.equalsIgnoreCase(value)) {
                return m;
            }
        }
        throw new IllegalArgumentException("No constant with text " + value + " found");
    }
}
