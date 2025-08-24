package com.nr.mq.publisher;

import com.nr.mq.pb.MessageQueueProtos;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@DependsOn("TCPServer")
public class Publisher {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8085;
    private static final long PUBLISH_INTERVAL_SECONDS = 5;

    private Socket socket;
    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            log.info("Connected to TCP server at {}:{}", SERVER_ADDRESS, SERVER_PORT);

            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            // Schedule a task to send a message every 5 seconds
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    MessageQueueProtos.ClientRequest request = MessageQueueProtos.ClientRequest.newBuilder()
                            .setAction("PUBLISH")
                            .setContent("Hello from Publisher")
                            .build();

                    log.info("Sending message: {}", request);
                    request.writeTo(outputStream);

                    MessageQueueProtos.ClientResponse response = MessageQueueProtos.ClientResponse.parseFrom(inputStream);
                    log.info("Received response: {}", response);

                } catch (IOException e) {
                    log.error("Error reading response from server", e);
                }
            }, 0, PUBLISH_INTERVAL_SECONDS, TimeUnit.SECONDS);

        } catch (IOException e) {
            log.error("Could not connect to server", e);
            // If we can't connect, we should shut down the scheduler
            if (scheduler != null) {
                scheduler.shutdown();
            }
        }
    }

    @PreDestroy
    public void stop() {
        if (socket != null && socket.isConnected()) {
            try {
                log.info("Closing TCP server at {}:{}", SERVER_ADDRESS, SERVER_PORT);
                socket.close();
            } catch (IOException e) {
                log.error("Could not close TCP server at {}:{}", SERVER_ADDRESS, SERVER_PORT);
                throw new RuntimeException(e);
            }
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            log.info("Shutting down publisher scheduler");
            scheduler.shutdown();
        }
    }
}
