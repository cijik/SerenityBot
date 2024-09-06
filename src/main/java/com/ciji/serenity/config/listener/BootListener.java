package com.ciji.serenity.config.listener;

import com.ciji.serenity.config.Client;
import com.ciji.serenity.config.CommandInitializer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

public class BootListener implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        event.getApplicationContext().getBean(Client.class).init();
        event.getApplicationContext().getBean(CommandInitializer.class).initializeCommands();
    }
}
