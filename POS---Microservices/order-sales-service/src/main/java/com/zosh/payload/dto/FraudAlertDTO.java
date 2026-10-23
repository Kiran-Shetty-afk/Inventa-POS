package com.zosh.payload.dto;

import java.time.LocalDateTime;

public record FraudAlertDTO(
        Long id,
        Long orderId,
        Long branchId,
        double riskScore,
        String riskLevel,
        String reason,
        String status,
        LocalDateTime createdAt
) {
}
