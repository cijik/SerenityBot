package com.ciji.serenity.service;

import discord4j.core.object.entity.Message;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Service
public class TimerService {

    private StopWatch stopWatch;

    public Mono<Void> getTimer(Message eventMessage) {
        try {
            long hours = stopWatch.getTime(TimeUnit.HOURS);
            long minutes = stopWatch.getTime(TimeUnit.MINUTES) % 60;
            long seconds = stopWatch.getTime(TimeUnit.SECONDS) % 60;
            return Mono.just(eventMessage)
                    .flatMap(Message::getChannel)
                    .flatMap(channel -> channel.createMessage("Current timer: " + hours + ":" + minutes + ":" + seconds))
                    .then();
        } catch (NullPointerException e) {
            return Mono.just(eventMessage)
                    .flatMap(Message::getChannel)
                    .flatMap(channel -> channel.createMessage("No timer active at the moment."))
                    .then();
        }
    }

    public Mono<Void> startTimer(Message eventMessage) {
        stopWatch = StopWatch.createStarted();
        return Mono.just(eventMessage)
                .flatMap(Message::getChannel)
                .flatMap(channel -> channel.createMessage("Timer started."))
                .then();
    }

    public Mono<Void> stopTimer(Message eventMessage) {
        stopWatch.stop();
        return Mono.just(eventMessage)
                .flatMap(Message::getChannel)
                .flatMap(channel -> channel.createMessage("Timer stopped."))
                .then();
    }
}
