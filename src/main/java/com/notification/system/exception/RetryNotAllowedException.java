package com.notification.system.exception;

/** Thrown when a retry is requested but one of the three eligibility rules fails. */
public class RetryNotAllowedException extends RuntimeException {

    public RetryNotAllowedException(String reason) {
        super(reason);
    }
}
