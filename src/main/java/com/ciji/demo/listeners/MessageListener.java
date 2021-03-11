package com.ciji.demo.listeners;

import com.ciji.demo.service.CommandProcessingService;
import discord4j.core.object.entity.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Component
public abstract class MessageListener {

    @Autowired
    private CommandProcessingService processingService;
    
    public Mono<Void> processCommand(Message eventMessage) {
        return processingService.processCommand(eventMessage);
    }
}
