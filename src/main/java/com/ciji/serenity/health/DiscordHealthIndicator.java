package com.ciji.serenity.health;

import com.ciji.serenity.config.Client;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DiscordHealthIndicator implements HealthIndicator {

    private final Client client;

    public DiscordHealthIndicator(Client client) {
        this.client = client;
    }

    @Override
    public Health health() {
        try {
            Optional<Integer> connectedCount = client.getClient().getGatewayResources().getShardCoordinator().getConnectedCount().blockOptional();
            if (connectedCount.isPresent() && connectedCount.get() > 0) {
                return Health.up()
                        .withDetail("status", "connected")
                        .withDetail("gateway", "active")
                        .build();
            } else {
                return Health.down()
                        .withDetail("status", "disconnected")
                        .withDetail("gateway", "inactive")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
