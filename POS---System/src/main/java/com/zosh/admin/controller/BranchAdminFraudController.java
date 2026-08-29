package com.zosh.admin.controller;

import com.zosh.admin.dto.BranchAdminFraudDashboardDTO;
import com.zosh.admin.dto.FraudTrendDTO;
import com.zosh.admin.dto.RiskDistributionDTO;
import com.zosh.admin.service.BranchAdminFraudService;
import com.zosh.fraud.dto.FraudAlertDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/branch-admin/fraud")
@RequiredArgsConstructor
public class BranchAdminFraudController {

    private final BranchAdminFraudService service;

    @GetMapping("/dashboard")
    public BranchAdminFraudDashboardDTO dashboard() {
        return service.getDashboard();
    }
    @GetMapping("/recent-alerts")
    public List<FraudAlertDTO> getRecentAlerts() {
        return service.getRecentAlerts();
    }
    @GetMapping("/trend")
    public List<FraudTrendDTO> trend() {
        return service.getFraudTrend();
    }
    @GetMapping("/risk-distribution")
    public List<RiskDistributionDTO> riskDistribution() {
        return service.getRiskDistribution();
    }

}