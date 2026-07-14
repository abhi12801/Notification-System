package com.notification.system.dto.request;

import com.notification.system.enums.NotificationType;
import com.notification.system.validation.NoRepeatedWords;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateNotificationRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "type is required")
    private NotificationType type;

    @NotBlank(message = "message must not be blank")
    @Size(max = 1000, message = "message must not exceed 1000 characters")
    @NoRepeatedWords
    private String message;

    @FutureOrPresent(message = "scheduleTime must not be in the past")
    private LocalDateTime scheduleTime;
}
