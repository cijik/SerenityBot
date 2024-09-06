package com.ciji.serenity.service.adapter;

import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.lifecycle.ConnectEvent;
import discord4j.core.event.domain.lifecycle.ReconnectEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;

@Component
public class ConnectionEventAdapter extends ReactiveEventAdapter {

    @Override
    public Publisher<?> onConnect(ConnectEvent event) {
        return event.getClient().updatePresence(ClientPresence.of(Status.DO_NOT_DISTURB, ClientActivity.custom("Loading commands...")));
    }

    @Override
    public Publisher<?> onReconnect(ReconnectEvent event) {
        return event.getClient().updatePresence(ClientPresence.of(Status.ONLINE, ClientActivity.listening("requests")));
    }
}
