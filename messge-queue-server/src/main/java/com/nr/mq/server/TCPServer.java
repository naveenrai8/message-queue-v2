package com.nr.mq.server;

import com.nr.mq.protocol.ProtocolManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@Getter
@RequiredArgsConstructor
public class TCPServer {


    private final ProtocolManager protocolManager;
    // Inject the server port from application.properties, default to 8085
    @Value("${tcp.server.port:8085}")
    private int port;
    private ServerSocket serverSocket;
    private ExecutorService clientHandlerPool;
    private volatile boolean isRunning = true;

    /**
     * This method is called by Spring after the bean is constructed.
     * It starts the server in a new thread to avoid blocking the main application startup.
     */
    @PostConstruct
    public void start() {
        // Use a cached thread pool for handling multiple clients concurrently
        this.clientHandlerPool = Executors.newCachedThreadPool();

        // Start the server's main listening loop in a separate thread
        Thread serverThread = new Thread(this::runServer);
        serverThread.setName("tcp-server-thread");
        serverThread.start();
    }

    private void runServer() {
        try {
            this.serverSocket = new ServerSocket(port);
            log.info("TCP Server started on port: {}", port);

            // Main loop to accept client connections
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    log.info("New client connected: {}", clientSocket.getInetAddress().getHostAddress());
                    // Submit the client handling task to the thread pool
                    clientHandlerPool.submit(new TCPClientManager(clientSocket, protocolManager));
                } catch (IOException e) {
                    if (!isRunning) {
                        log.info("Server socket closed, shutting down.");
                        break; // Exit loop if server is shutting down
                    }
                    log.error("Error accepting client connection", e);
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                log.error("Could not start TCP server on port {}", port, e);
            }
        } finally {
            // Ensure the client handler pool is shut down when the server loop exits
            if (!clientHandlerPool.isShutdown()) {
                clientHandlerPool.shutdown();
            }
        }
    }

    /**
     * This method is called by Spring during application shutdown.
     * It ensures a graceful shutdown of the server socket and the client handler thread pool.
     */
    @PreDestroy
    public void stop() {
        log.info("Shutting down TCP server...");
        this.isRunning = false;

        try {
            // Close the server socket to stop accepting new connections
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.error("Error closing server socket", e);
        }

        // Gracefully shut down the client handler thread pool
        clientHandlerPool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!clientHandlerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Client handler pool did not terminate in 5 seconds. Forcing shutdown...");
                clientHandlerPool.shutdownNow(); // Cancel currently executing tasks
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            clientHandlerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("TCP server shut down successfully.");
    }
}
