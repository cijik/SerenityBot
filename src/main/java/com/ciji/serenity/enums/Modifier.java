package com.ciji.serenity.enums;

import lombok.Getter;

@Getter
public enum Modifier {
    TWO("2"),
    ONE_AND_ONE_HALF("1 1/2"),
    ONE("1"),
    THREE_QUARTERS("3/4"),
    ONE_HALF("1/2"),
    ONE_QUARTER("1/4"),
    ONE_TENTH("1/10");

    private final String modifier;

    Modifier(String modifier) {
        this.modifier = modifier;
    }

    public static Modifier fromString(String value) {
        for (Modifier m : Modifier.values()) {
            if (m.modifier.equalsIgnoreCase(value)) {
                return m;
            }
        }
        throw new IllegalArgumentException("No constant with text " + value + " found");
    }
}
