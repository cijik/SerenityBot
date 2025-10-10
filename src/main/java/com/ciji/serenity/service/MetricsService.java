package com.ciji.serenity.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Counter commandCounter;
    private final Counter errorCounter;
    private final Counter reconnectionCounter;
    private final Timer commandProcessingTime;
    private final MeterRegistry meterRegistry;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.commandCounter = Counter.builder("discord.commands.total")
                .description("Total number of commands processed")
                .register(meterRegistry);
        
        this.errorCounter = Counter.builder("discord.errors.total")
                .description("Total number of errors")
                .register(meterRegistry);
        
        this.reconnectionCounter = Counter.builder("discord.reconnections.total")
                .description("Total number of reconnections to Discord")
                .register(meterRegistry);
        
        this.commandProcessingTime = Timer.builder("discord.command.processing.time")
                .description("Time taken to process commands")
                .register(meterRegistry);
    }

    public void incrementCommandCount() {
        commandCounter.increment();
    }

    public void incrementErrorCount() {
        errorCounter.increment();
    }

    public void incrementReconnectionCount() {
        reconnectionCounter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordCommandProcessingTime(Timer.Sample sample) {
        sample.stop(commandProcessingTime);
    }
}
