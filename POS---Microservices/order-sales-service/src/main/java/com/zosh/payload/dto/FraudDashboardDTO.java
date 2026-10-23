package com.zosh.payload.dto;

public record FraudDashboardDTO(
        long totalAlerts,
        long criticalAlerts,
        long highAlerts,
        double averageRiskScore
) {
}
