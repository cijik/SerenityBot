package com.ciji.serenity.service;

import com.ciji.serenity.calendar.EquestrianClock;
import com.ciji.serenity.calendar.EquestrianDate;
import com.ciji.serenity.dao.BotParameterDao;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.discordjson.json.MessageData;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TimerService {

    private final BotParameterDao botParameterDao;

    private final StopWatch stopWatch = StopWatch.create();

    private final Timer timer = new Timer();

    private boolean timerIsRunning = false;

    //TODO: add commands to resume the clock from DB
    private EquestrianClock clock;

    public Mono<MessageData> getTime(ApplicationCommandInteractionEvent event) {
        //TODO: retrieve data from the clock, both the DB or current, maybe split into two methods
        try {
            long hours = stopWatch.getTime(TimeUnit.HOURS);
            long minutes = stopWatch.getTime(TimeUnit.MINUTES) % 60;
            long seconds = stopWatch.getTime(TimeUnit.SECONDS) % 60;
            return event.reply()
                    .then(event.getInteractionResponse()
                            .createFollowupMessage("Current timer: " + hours + ":" + minutes + ":" + seconds));
        } catch (NullPointerException e) {
            return postMessage(event, "No timer active at the moment.");
        }
    }

    public Mono<MessageData> startClock(ApplicationCommandInteractionEvent event) {
        if (!timerIsRunning) {
            clock = new EquestrianClock(0);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    clock.tick();
                    checkDayCounter();
                }
            }, 0, 1000);
            timerIsRunning = true;
            return postMessage(event, "Clock started.");
        } else {
            return postMessage(event, "Clock is already running.");
        }
    }

    public Mono<MessageData> stopClock(ApplicationCommandInteractionEvent event) {
        if (timerIsRunning) {
            timer.cancel();
            timerIsRunning = false;
            return postMessage(event, "Clock stopped.");
        } else {
            return postMessage(event, "No clock is running at the moment.");
        }
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

    private Mono<MessageData> postMessage(ApplicationCommandInteractionEvent event, String message) {
        return event.reply()
                .then(event.getInteractionResponse()
                        .createFollowupMessage(message));
    }
}
