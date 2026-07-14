package com.notification.system.controller;

import com.notification.system.dto.response.DashboardResponse;
import com.notification.system.response.ApiResponse;
import com.notification.system.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getStatistics() {
        return ResponseEntity.ok(ApiResponse.of(dashboardService.getStatistics()));
    }
}
