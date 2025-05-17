package com.ciji.serenity.service.context;

public class SchedulerContextHolder {
    private static final ThreadLocal<Boolean> schedulerContext = new ThreadLocal<>();

    public static void setSchedulerContext(boolean isScheduled) {
        schedulerContext.set(isScheduled);
    }

    public static boolean isCalledFromScheduler() {
        return Boolean.TRUE.equals(schedulerContext.get());
    }

    public static void clear() {
        schedulerContext.remove();
    }
}
