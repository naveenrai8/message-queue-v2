package com.nr.mq.server;

import com.nr.mq.pb.MessageQueueProtos;
import com.nr.mq.protocol.ProtocolManager;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
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
        try (
                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream()
        ) {
            while (clientSocket.isConnected()) {
                MessageQueueProtos.ClientRequest request = MessageQueueProtos.ClientRequest.parseFrom(inputStream);
                log.info("Received from client {}: {}", clientSocket.getInetAddress().getHostAddress(), request);
                MessageQueueProtos.ClientResponse response = this.parseResponse(request);
                response.writeTo(outputStream);
            }
        } catch (IOException e) {
            // This exception is expected when a client disconnects
            log.info("Client {} disconnected.", clientSocket.getInetAddress().getHostAddress());
        } finally {
            log.info("Closing connection for client: {}", clientSocket.getInetAddress().getHostAddress());
        }
    }

    private MessageQueueProtos.ClientResponse parseResponse(MessageQueueProtos.ClientRequest request) {
        MessageQueueProtos.ClientResponse.Builder responseBuilder = MessageQueueProtos.ClientResponse.newBuilder();
        try {
            log.info("Received from client {}", request);
            responseBuilder.addMessages(MessageQueueProtos.Message.newBuilder()
                    .setMessageId(UUID.randomUUID().toString())
                    .setMessage(request.hasContent() ? request.getContent() : "Dummy message")
                    .build());
        } catch (Exception e) {
            log.error("Error parsing response from client {}", request, e);
            responseBuilder.setError(MessageQueueProtos.Error.newBuilder()
                    .setMessage(e.getMessage())
                    .build());
        }
        return responseBuilder.build();
    }
}
