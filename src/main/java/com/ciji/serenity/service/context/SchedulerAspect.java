package com.ciji.serenity.service.context;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SchedulerAspect {

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object markScheduledExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            SchedulerContextHolder.setSchedulerContext(true);
            return joinPoint.proceed();
        } finally {
            SchedulerContextHolder.clear();
        }
    }
}
