package com.notification.system.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.Getter;

/** Consistent envelope for every successful API response. */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final LocalDateTime timestamp;

    private ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> of(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }
}
