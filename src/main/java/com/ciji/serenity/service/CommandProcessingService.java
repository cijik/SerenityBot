package com.ciji.serenity.service;

import com.ciji.serenity.dao.BotParameterDao;
import com.ciji.serenity.enums.Commands;
import com.ciji.serenity.model.BotParam;

import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@Slf4j
public class CommandProcessingService {
    
    @Autowired
    private BotParameterDao botParameterDao;

    @Autowired
    private TimerService timerService;

    @Autowired
    private DateService dateService;

    private static String PREFIX;

    public BotParam getParameter(String id) {
        return botParameterDao.findById(id).orElseThrow(RuntimeException::new);
    }

    public Mono<Void> processCommand(Message eventMessage) {
        Optional<BotParam> botParam = botParameterDao.findById("prefix");
        if (botParam.isPresent()) {
            PREFIX = botParam.get().getValue();
        } else {
            BotParam defaultParam = new BotParam();
            defaultParam.setName("prefix");
            defaultParam.setValue("!");
            botParameterDao.save(defaultParam);
            PREFIX = "!";
        }

        if (eventMessage.getContent().matches("^"+PREFIX+"[a-zA-Z ]+[A-Za-z0-9.]*$")) {
            switch (Commands.fromString(eventMessage.getContent().split(" ")[0].substring(PREFIX.length()))) {
                case TODO: return doTodo(eventMessage);
                case GET_TIMER: return timerService.getTimer(eventMessage);
                case START_TIMER: return timerService.startTimer(eventMessage);
                case STOP_TIMER: return timerService.stopTimer(eventMessage);
                case SET_PREFIX: return setPrefix(eventMessage);
                case SET_DATE: return dateService.setDate(eventMessage);
                case GET_DATE: return dateService.getDate(eventMessage);
                case ADD_DAYS: return dateService.addDays(eventMessage);
            }
        } else {
            log.error("Invalid prefix!");
        }

        return Mono.empty();
    }

    private Mono<Void> doTodo(Message eventMessage) {
        return Mono.just(eventMessage)
                .flatMap(Message::getChannel)
                .flatMap(channel -> channel.createMessage("Things to do today:\n - write a bot\n - eat lunch\n - play a game"))
                .then();
    }

    private Mono<Void> setPrefix(Message eventMessage) {
        String newPrefix = eventMessage.getContent().split(" ")[1];
        botParameterDao.findById("prefix").ifPresent(param -> {
            param.setValue(newPrefix);
            botParameterDao.save(param);
        });
        return Mono.just(eventMessage)
                .flatMap(Message::getChannel)
                .flatMap(channel -> channel.createMessage("New prefix set: " + newPrefix))
                .then();
    }
}
