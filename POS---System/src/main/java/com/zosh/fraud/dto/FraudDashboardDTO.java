package com.zosh.fraud.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudDashboardDTO {

    private Long totalAlerts;

    private Long criticalAlerts;

    private Long highAlerts;

    private Long mediumAlerts;

    private Long lowAlerts;

    private Double averageRiskScore;


}