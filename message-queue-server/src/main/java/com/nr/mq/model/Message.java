package com.nr.mq.model;


import lombok.Builder;

/**
 * Represents a single message with its ID.
 *
 * @param messageId A unique identifier for the message.
 * @param message The string content of the message.
 */
@Builder
public record Message(String messageId, String message) {}
