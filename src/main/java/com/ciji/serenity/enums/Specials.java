package com.ciji.serenity.enums;

public enum Specials {

    STRENGTH("S"),
    PERCEPTION("P"),
    ENDURANCE("E"),
    CHARISMA("C"),
    INTELLIGENCE("I"),
    AGILITY("A"),
    LUCK("L");

    private final String literal;

    Specials(String literal) {
        this.literal = literal;
    }

    public static Specials fromString(String value) {
        for (Specials s : Specials.values()) {
            if (s.literal.equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("No constant with text " + value + " found");
    }
}
