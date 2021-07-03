package com.ciji.serenity.calendar;

import lombok.Getter;
import lombok.Setter;

public class EquestrianClock {

    @Getter
    @Setter
    private int ticks;

    @Getter
    private int days;

    public EquestrianClock(int ticks) {
        this.ticks = ticks;
    }

    public void tick() {
        ticks++;
        if (ticks >= 86400) {
            convertAndResetTicks();
        }
    }

    public void flushDayCounter() {
        days = 0;
    }

    public static EquestrianClock fromSeconds(int seconds) {
        return new EquestrianClock(seconds);
    }

    public static EquestrianClock fromTimeString(String time) {
        String[] timeValues = time.split(":");
        int totalSeconds = Integer.parseInt(timeValues[0]) * 3600 +
                Integer.parseInt(timeValues[1]) * 60 +
                Integer.parseInt(timeValues[2]);
        return new EquestrianClock(totalSeconds);
    }

    private void convertAndResetTicks() {
        days++;
        ticks -= 86400;
    }
}
