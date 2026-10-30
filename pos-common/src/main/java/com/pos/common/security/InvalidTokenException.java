package com.pos.common.security;

/** Thrown when a JWT is missing, malformed, expired or has a bad signature. */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
