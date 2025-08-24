package com.nr.mq.health;

import com.nr.mq.server.TCPServer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

@RequiredArgsConstructor
public class TCPServerHealth implements HealthIndicator {

    private final TCPServer tcpServer;

    @Override
    public Health health() {
        if (tcpServer.isRunning()) {
            return Health.up()
                    .withDetail("Server is running", tcpServer.isRunning())
                    .build();
        }
        return Health.down().build();
    }
}
