package com.notification.system.exception;

public class DuplicateNotificationException extends RuntimeException {

    public DuplicateNotificationException(String message) {
        super(message);
    }
}
