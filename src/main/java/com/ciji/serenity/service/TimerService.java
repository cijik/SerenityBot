package com.ciji.serenity.service;

import com.ciji.serenity.calendar.EquestrianClock;
import com.ciji.serenity.calendar.EquestrianDate;
import com.ciji.serenity.dao.BotParameterDao;
import discord4j.core.event.domain.InteractionCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.MessageData;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@Service
public class TimerService {

    @Autowired
    private BotParameterDao botParameterDao;

    private StopWatch stopWatch;

    private final Timer timer = new Timer();

    //TODO: add commands to resume the clock from DB
    private EquestrianClock clock;

    public Mono<MessageData> getTime(InteractionCreateEvent event) {
        //TODO: retrieve data from the clock, both the DB or current, maybe split into two methods
        try {
            long hours = stopWatch.getTime(TimeUnit.HOURS);
            long minutes = stopWatch.getTime(TimeUnit.MINUTES) % 60;
            long seconds = stopWatch.getTime(TimeUnit.SECONDS) % 60;
            return event.acknowledge()
                    .then(event.getInteractionResponse()
                            .createFollowupMessage("Current timer: " + hours + ":" + minutes + ":" + seconds));
        } catch (NullPointerException e) {
            return postMessage(event, "No timer active at the moment.");
        }
    }

    public Mono<MessageData> startClock(InteractionCreateEvent event) {
        clock = new EquestrianClock(0);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                clock.tick();
                checkDayCounter();
            }
        }, 0, 1000);
        return postMessage(event, "Clock started.");
    }

    public Mono<MessageData> stopClock(InteractionCreateEvent event) {
        timer.cancel();
        return postMessage(event, "Timer stopped.");
    }

    private void checkDayCounter() {
        if (clock.getDays() > 0) {
            botParameterDao.findById("equestrian_date").ifPresent(date -> {
                EquestrianDate eqDate = EquestrianDate.fromDateString(date.getValue());
                eqDate.addDays(clock.getDays());
                date.setValue(String.format("%d.%d.%d", eqDate.getDay(), eqDate.getMonth(), eqDate.getYear()));
                botParameterDao.save(date);
                clock.flushDayCounter();
            });
        }
    }

    private Mono<MessageData> postMessage(InteractionCreateEvent event, String message) {
        return event.acknowledge()
                .then(event.getInteractionResponse()
                        .createFollowupMessage(message));
    }
}
