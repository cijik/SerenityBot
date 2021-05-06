package com.ciji.serenity.service;

import com.ciji.serenity.enums.Commands;
import com.ciji.serenity.exception.OptionNotFoundException;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.InteractionCreateEvent;
import discord4j.discordjson.json.MessageData;
import lombok.AllArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class SerenityEventAdapter extends ReactiveEventAdapter {

    private final TimerService timerService;

    private final DateService dateService;

    @Override
    public Publisher<?> onInteractionCreate(InteractionCreateEvent event) {
        try {
            switch (Commands.fromString(event.getCommandName())) {
                case TODO: return doTodo(event);
                case GET_TIMER: return timerService.getTimer(event);
                case START_TIMER: return timerService.startTimer(event);
                case STOP_TIMER: return timerService.stopTimer(event);
                case SET_DATE: return dateService.setDate(event);
                case GET_DATE: return dateService.getDate(event);
                case ADD_DAYS: return dateService.addDays(event);
            }
        } catch (OptionNotFoundException e) {
            return event.acknowledge()
                    .then(event.getInteractionResponse()
                            .createFollowupMessage("No option specified!"));
        }
        return Mono.empty();
    }

    private Mono<MessageData> doTodo(InteractionCreateEvent event) {
        return event.acknowledge()
                .then(event.getInteractionResponse()
                        .createFollowupMessage("Things to do today:\n - write a bot\n - eat lunch\n - play a game"));
    }
}
