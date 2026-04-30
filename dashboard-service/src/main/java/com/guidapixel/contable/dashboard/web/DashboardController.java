package com.guidapixel.contable.dashboard.web;

import com.guidapixel.contable.dashboard.service.DashboardService;
import com.guidapixel.contable.dashboard.web.dto.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(dashboardService.getDashboardData(authHeader));
    }
}
