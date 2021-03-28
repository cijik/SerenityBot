package com.ciji.serenity.calendar;

public enum EquestrianMonth {

    FIRST,
    SECOND,
    THIRD,
    FOURTH,
    FIFTH,
    SIXTH,
    SEVENTH,
    EIGHTH,
    NINTH,
    TENTH,
    ELEVENTH,
    TWELFTH,
    THIRTEENTH;


    @Override
    public String toString() {
        String monthName = this.name();
        return monthName.substring(0, 1).toUpperCase() + monthName.substring(1).toLowerCase();
    }
}
