package com.nr.mq.health;

import com.nr.mq.server.NettyServer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NettyServerHealth implements HealthIndicator {

    private final NettyServer nettyServer;

    @Override
    public Health health() {
        if (!nettyServer.getBossGroup().isShutdown()) {
            return Health.up()
                    .withDetail("Server is running", !nettyServer.getBossGroup().isShutdown())
                    .build();
        }
        return Health.down().build();
    }
}
