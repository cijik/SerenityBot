package com.ciji.serenity.service;

import discord4j.core.event.domain.InteractionCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.MessageData;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Service
public class TimerService {

    private StopWatch stopWatch;

    public Mono<MessageData> getTimer(InteractionCreateEvent event) {
        try {
            long hours = stopWatch.getTime(TimeUnit.HOURS);
            long minutes = stopWatch.getTime(TimeUnit.MINUTES) % 60;
            long seconds = stopWatch.getTime(TimeUnit.SECONDS) % 60;
            return event.acknowledge()
                    .then(event.getInteractionResponse()
                            .createFollowupMessage("Current timer: " + hours + ":" + minutes + ":" + seconds));
        } catch (NullPointerException e) {
            return event.acknowledge()
                    .then(event.getInteractionResponse()
                            .createFollowupMessage("No timer active at the moment."));
        }
    }

    public Mono<MessageData> startTimer(InteractionCreateEvent event) {
        stopWatch = StopWatch.createStarted();
        return event.acknowledge()
                .then(event.getInteractionResponse()
                        .createFollowupMessage("Timer started."));
    }

    public Mono<MessageData> stopTimer(InteractionCreateEvent event) {
        stopWatch.stop();
        return event.acknowledge()
                .then(event.getInteractionResponse()
                        .createFollowupMessage("Timer stopped."));
    }
}
