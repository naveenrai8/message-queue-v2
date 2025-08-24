package com.nr.mq.server;

import com.nr.mq.model.ClientResponse;
import com.nr.mq.model.Error;
import com.nr.mq.model.Message;
import com.nr.mq.protocol.ClientRequest;
import com.nr.mq.protocol.ProtocolManager;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.UUID;


/**
 * A Runnable task to handle communication with a single client.
 */
@Slf4j
public class TCPClientManager implements Runnable {
    private final Socket clientSocket;
    private final ProtocolManager protocolManager;

    public TCPClientManager(Socket socket, ProtocolManager protocolManager) {
        this.clientSocket = socket;
        this.protocolManager = protocolManager;
    }

    @Override
    public void run() {
        // Use try-with-resources to ensure streams and socket are closed automatically
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("Received from client {}: {}", clientSocket.getInetAddress().getHostAddress(), line);
                var response = this.parseResponse(line);
                // Echo the message back to the client
                writer.println("Echo: " + response);
            }
        } catch (IOException e) {
            // This exception is expected when a client disconnects
            log.info("Client {} disconnected.", clientSocket.getInetAddress().getHostAddress());
        } finally {
            log.info("Closing connection for client: {}", clientSocket.getInetAddress().getHostAddress());
        }
    }

    private String parseResponse(String input) {
        ClientResponse clientResponse = ClientResponse.builder().build();
        Message message = null;
        Error error = null;
        try {
            ClientRequest request = this.protocolManager.parse(input);
            log.info("Received from client {}", request);
            clientResponse.setMessages(List.of(Message.builder()
                    .messageId(UUID.randomUUID().toString())
                    .message(request.content().orElse("Dummy message"))
                    .build()));
        } catch (Exception e) {
            log.error("Error parsing response from client {}", input, e);
            clientResponse.setError(Error.builder()
                    .message(e.getMessage())
                    .build());
        }
        return this.protocolManager.serialize(clientResponse);
    }
}
