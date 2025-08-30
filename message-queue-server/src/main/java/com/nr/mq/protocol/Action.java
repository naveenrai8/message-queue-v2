package com.nr.mq.protocol;

/**
 * Defines the possible actions a client can request.
 */
public enum Action {
    PUBLISH,
    READ,
    DELETE,
    EXTEND_LEASE
}
