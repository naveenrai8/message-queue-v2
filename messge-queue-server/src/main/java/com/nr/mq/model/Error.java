package com.nr.mq.model;

import lombok.Builder;

/**
 * Represents a serializable error.
 *
 * @param code    A numerical error code.
 * @param message A descriptive error message.
 */
@Builder
public record Error(int code, String message) {
}
