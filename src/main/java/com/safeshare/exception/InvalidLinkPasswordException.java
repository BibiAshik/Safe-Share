package com.safeshare.exception;

public class InvalidLinkPasswordException extends RuntimeException {
    public InvalidLinkPasswordException(String message) {
        super(message);
    }
}
