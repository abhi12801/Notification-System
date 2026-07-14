package com.notification.system.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

/** Consistent envelope for every error response, regardless of the cause. */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final boolean success = false;
    private final String errorCode;
    private final String message;
    private final List<String> details;
    private final LocalDateTime timestamp;

    public ErrorResponse(String errorCode, String message, List<String> details) {
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(String errorCode, String message) {
        this(errorCode, message, null);
    }
}
