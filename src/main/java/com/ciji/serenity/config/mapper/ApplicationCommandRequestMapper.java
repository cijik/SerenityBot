package com.ciji.serenity.config.mapper;

import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;

public class ApplicationCommandRequestMapper {

    public static ApplicationCommandRequest map(ApplicationCommandData commandData) {
        return ApplicationCommandRequest.builder()
                .name(commandData.name())
                .description(commandData.description())
                .type(commandData.type())
                .build();
    }
}
