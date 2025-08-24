package com.nr.mq.protocol;

import com.nr.mq.model.ClientResponse;
import com.nr.mq.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProtocolManager {

    /**
     * Parses a raw string command into a structured ClientRequest object.
     * The command format is a series of key-value pairs separated by "~SEP~",
     * e.g., "Action=PUBLISH~SEP~Content=Hello World".
     *
     * @param input The raw command string from the client.
     * @return A populated ClientRequest record.
     * @throws IllegalArgumentException if the input is null, malformed, missing required fields,
     *                                  or contains invalid values.
     */
    public ClientRequest parse(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input string cannot be null or empty.");
        }

        Map<String, String> params = parseToMap(input);

        String actionStr = params.get("Action");
        if (actionStr == null) {
            throw new IllegalArgumentException("Missing mandatory 'Action' parameter.");
        }

        Action action;
        try {
            action = Action.valueOf(actionStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid action specified: " + actionStr);
        }

        return switch (action) {
            case PUBLISH -> buildPublishRequest(params);
            case READ -> buildReadRequest(params);
            case EXTEND_LEASE -> buildExtendLeaseRequest(params);
            case DELETE -> buildDeleteRequest(params);
        };
    }

    /**
     * Serializes a ClientResponse object into a string using the custom protocol format.
     *
     * @param response The ClientResponse object to serialize.
     * @return A string formatted as "Key1=Value1~SEP~Key2=Value2...".
     */
    public String serialize(ClientResponse response) {
        // Using LinkedHashMap to maintain a predictable order of fields in the output string.
        Map<String, String> fields = new LinkedHashMap<>();

        // Add error fields if present
        if (response.getError() != null) {
            fields.put("ErrorCode", String.valueOf(response.getError().code()));
            fields.put("ErrorMessage", response.getError().message());
        }

        // Add ClientId if present
        if (response.getClientId() != null) {
            fields.put("ClientId", response.getClientId());
        }

        // Add ClientId if present
        if (response.getClientId() != null) {
            fields.put("LeaseExpiredAt", Long.toString(response.getLeaseExpiredAt().getEpochSecond()));
        }

        // Add messages using indexed keys
        List<Message> messages = response.getMessages();
        if (messages != null && !messages.isEmpty()) {
            for (int i = 0; i < messages.size(); i++) {
                Message msg = messages.get(i);
                fields.put("MessageId_" + i, msg.messageId());
                fields.put("Message_" + i, msg.message());
            }
        }

        // Build the final string
        return fields.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("~SEP~"));
    }

    private Map<String, String> parseToMap(String input) {
        try {
            // --- MODIFIED LINE ---
            // Changed the delimiter from ";" to "~SEP~"
            return Arrays.stream(input.split("~SEP~"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(pair -> pair.split("=", 2))
                    .collect(Collectors.toMap(
                            parts -> parts[0].trim(),
                            parts -> parts[1].trim()
                    ));
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Malformed key-value pair found in input: " + input, e);
        }
    }

    private ClientRequest buildPublishRequest(Map<String, String> params) {
        String content = params.get("Content");
        if (content == null) {
            throw new IllegalArgumentException("Missing 'Content' for PUBLISH action.");
        }
        return new ClientRequest(Action.PUBLISH, Optional.of(content), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private ClientRequest buildReadRequest(Map<String, String> params) {
        String clientId = getRequiredParam(params, "ClientId");
        Optional<Long> lease = parseOptionalLong(params, "LeaseExpiredAt");
        return new ClientRequest(Action.READ, Optional.empty(), Optional.of(clientId), Optional.empty(), lease);
    }

    private ClientRequest buildExtendLeaseRequest(Map<String, String> params) {
        String clientId = getRequiredParam(params, "ClientId");
        Optional<Long> lease = parseOptionalLong(params, "LeaseExpiredAt");
        return new ClientRequest(Action.EXTEND_LEASE, Optional.empty(), Optional.of(clientId), Optional.empty(), lease);
    }

    private ClientRequest buildDeleteRequest(Map<String, String> params) {
        String clientId = getRequiredParam(params, "ClientId");
        String messageId = getRequiredParam(params, "MessageId");
        return new ClientRequest(Action.DELETE, Optional.empty(), Optional.of(clientId), Optional.of(messageId), Optional.empty());
    }

    private String getRequiredParam(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing mandatory '" + key + "' parameter.");
        }
        return value;
    }

    private Optional<Long> parseOptionalLong(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format for '" + key + "': " + value, e);
        }
    }
}
