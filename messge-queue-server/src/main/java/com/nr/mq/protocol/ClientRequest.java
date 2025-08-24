package com.nr.mq.protocol;

import java.util.Optional;

/**
 * A record representing a parsed request from a client.
 * Using a record provides immutability, equals(), hashCode(), and toString() automatically.
 *
 * @param action The mandatory action to be performed.
 * @param content The message content, present only for PUBLISH action.
 * @param clientId The client identifier, required for READ, DELETE, and EXTEND_LEASE.
 * @param messageId The message identifier, required only for DELETE.
 * @param leaseExpiredAt An optional timestamp for READ and EXTEND_LEASE.
 */
public record ClientRequest(
        Action action,
        Optional<String> content,
        Optional<String> clientId,
        Optional<String> messageId,
        Optional<Long> leaseExpiredAt
) {}
