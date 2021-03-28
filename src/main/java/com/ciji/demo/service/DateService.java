package com.ciji.demo.service;

import com.ciji.demo.calendar.EquestrianDate;
import com.ciji.demo.dao.BotParameterDao;
import com.ciji.demo.model.BotParam;
import discord4j.core.object.entity.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
public class DateService {

    @Autowired
    private BotParameterDao botParameterDao;

    public Mono<Void> setDate(Message eventMessage) {
        String dateValue = eventMessage.getContent().split(" ")[1];
        BotParam botParam = botParameterDao.findById("equestrian_date").orElse(new BotParam());
        EquestrianDate date = EquestrianDate.fromDateString(dateValue);
        botParam.setName("equestrian_date");
        botParam.setValue(dateValue);
        botParameterDao.save(botParam);
        return postMessage(eventMessage, "Current date set as: " + dateValue);
    }

    public Mono<Void> getDate(Message eventMessage) {
        Optional<BotParam> botParam = botParameterDao.findById("equestrian_date");
        if (botParam.isPresent()) {
            String dateValue = botParam.get().getValue();
            EquestrianDate date = EquestrianDate.fromDateString(dateValue);
            return postMessage(eventMessage, "Current date is: \n" +
                    "Year " + date.getYear() + "\n" +
                    "Month " + date.getMonthName() + "\n" +
                    "Day " + date.getDay());
        } else {
            return postMessage(eventMessage, "No date set!");
        }
    }

    public Mono<Void> addDays(Message eventMessage) {
        String days = eventMessage.getContent().split(" ")[1];
        Optional<BotParam> botParam = botParameterDao.findById("equestrian_date");
        if (botParam.isPresent()) {
            String dateValue = botParam.get().getValue();
            EquestrianDate date = EquestrianDate.fromDateString(dateValue);
            date.addDays(Integer.parseInt(days));
            return postMessage(eventMessage, "Days added.");
        } else {
            return postMessage(eventMessage, "No date set!");
        }
    }

    private Mono<Void> postMessage(Message eventMessage, String s) {
        return Mono.just(eventMessage)
                .flatMap(Message::getChannel)
                .flatMap(channel -> channel.createMessage(s))
                .then();
    }
}
