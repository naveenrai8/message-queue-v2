package com.nr.mq.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Represents a structured response to be sent to a client.
 *
 * @param error          An optional Error object if the request failed.
 * @param messages       A list of messages, typically for a READ action. Can be empty.
 * @param clientId       An optional client identifier.
 * @param leaseExpiredAt An optional lease expiration timestamp.
 */
@Builder
@Data
public class ClientResponse {
    private Error error;
    private List<Message> messages;
    private String clientId;
    private Instant leaseExpiredAt;
}
