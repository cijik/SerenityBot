package com.ciji.serenity.commands;

import discord4j.rest.RestClient;

public interface SerenityCommand {
    void register(long applicationId, RestClient restClient);
}
