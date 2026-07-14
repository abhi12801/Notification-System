package com.notification.system.controller;

import com.notification.system.dto.request.CreateNotificationRequest;
import com.notification.system.dto.response.NotificationResponse;
import com.notification.system.enums.NotificationStatus;
import com.notification.system.enums.NotificationType;
import com.notification.system.response.ApiResponse;
import com.notification.system.response.PagedResponse;
import com.notification.system.service.NotificationService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> create(
            @Valid @RequestBody CreateNotificationRequest request) {
        NotificationResponse created = notificationService.create(request);
        return ResponseEntity
                .created(URI.create("/api/notifications/" + created.getId()))
                .body(ApiResponse.of(created, "Notification created and queued for processing"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.of(notificationService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> list(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationResponse> page = notificationService.list(status, type, pageable);
        return ResponseEntity.ok(ApiResponse.of(new PagedResponse<>(page)));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<NotificationResponse>> retry(@PathVariable Long id) {
        NotificationResponse retried = notificationService.retry(id);
        return ResponseEntity.ok(ApiResponse.of(retried, "Retry accepted and queued for processing"));
    }
}
