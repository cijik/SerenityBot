package com.ciji.serenity.enums;

import java.util.Arrays;

public enum Specials {

    STRENGTH(new String[]{"S", "Str", "Strength"}),
    PERCEPTION(new String[]{"P", "Per", "Perception"}),
    ENDURANCE(new String[]{"E", "End", "Endurance"}),
    CHARISMA(new String[]{"C", "Cha", "Charisma"}),
    INTELLIGENCE(new String[]{"I", "Int", "Intelligence"}),
    AGILITY(new String[]{"A", "Agi", "Agility"}),
    LUCK(new String[]{"L", "Lck", "Luc", "Luk", "Luck"});

    private final String[] aliases;

    Specials(String[] aliases) {
        this.aliases = aliases;
    }

    public static Specials fromString(String value) {
        Specials result = null;
        for (Specials s : Specials.values()) {
            if (Arrays.stream(s.aliases).anyMatch(alias -> alias.equalsIgnoreCase(value))) {
                result = s;
                break;
            }
        }
        return result;
    }
}
