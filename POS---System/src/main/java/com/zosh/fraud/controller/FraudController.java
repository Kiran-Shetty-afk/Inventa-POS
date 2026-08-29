package com.zosh.fraud.controller;

import com.zosh.fraud.dto.FraudAlertDTO;
import com.zosh.fraud.dto.FraudDashboardDTO;
import com.zosh.fraud.service.FraudAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudAnalyticsService fraudAnalyticsService;

    @GetMapping("/dashboard")
    public FraudDashboardDTO dashboard() {
        return fraudAnalyticsService.getDashboard();
    }

    @GetMapping("/alerts")
    public List<FraudAlertDTO> getAllAlerts() {
        return fraudAnalyticsService.getAllAlerts();
    }

    @PutMapping("/resolve/{id}")
    public String resolveAlert(@PathVariable Long id) {
        fraudAnalyticsService.resolveAlert(id);
        return "Fraud Alert Resolved Successfully";
    }
}