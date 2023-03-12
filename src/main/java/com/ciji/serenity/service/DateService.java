package com.ciji.serenity.service;

import com.ciji.serenity.calendar.EquestrianDate;
import com.ciji.serenity.dao.BotParameterDao;
import com.ciji.serenity.exception.OptionNotFoundException;
import com.ciji.serenity.model.BotParam;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.MessageData;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@AllArgsConstructor
public class DateService {

    private final BotParameterDao botParameterDao;

    public Mono<MessageData> setDate(ChatInputInteractionEvent event) {
        String dateValue = event
                .getOption("date").orElseThrow(OptionNotFoundException::new)
                .getValue().orElseThrow()
                .asString();
        BotParam botParam = botParameterDao.findById("equestrian_date").orElse(new BotParam());
        botParam.setName("equestrian_date");
        botParam.setValue(dateValue);
        botParameterDao.save(botParam);
        return postMessage(event, "Current date set as: " + dateValue);
    }

    public Mono<MessageData> getDate(ChatInputInteractionEvent event) {
        Optional<BotParam> botParam = botParameterDao.findById("equestrian_date");
        if (botParam.isPresent()) {
            String dateValue = botParam.get().getValue();
            EquestrianDate date = EquestrianDate.fromDateString(dateValue);
            return postMessage(event, "Current date is: \n" +
                    "Year " + date.getYear() + "\n" +
                    "Month " + date.getMonthName() + "\n" +
                    "Day " + date.getDay());
        } else {
            return postMessage(event, "No date set!");
        }
    }

    public Mono<MessageData> addDays(ChatInputInteractionEvent event) {
        String days = event.getOption("days").orElseThrow(OptionNotFoundException::new).getValue().toString();
        Optional<BotParam> botParam = botParameterDao.findById("equestrian_date");
        if (botParam.isPresent()) {
            String dateValue = botParam.get().getValue();
            EquestrianDate date = EquestrianDate.fromDateString(dateValue);
            date.addDays(Integer.parseInt(days));
            botParam.get().setValue(String.format("%d.%d.%d", date.getDay(), date.getMonth(), date.getYear()));
            botParameterDao.save(botParam.get());
            return postMessage(event, "Days added.");
        } else {
            return postMessage(event, "No date set!");
        }
    }

    private Mono<MessageData> postMessage(ChatInputInteractionEvent event, String s) {
        return event.reply()
                .then(event.getInteractionResponse()
                        .createFollowupMessage(s));
    }
}
