package com.zosh.admin.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchFraudAnalyticsDTO {

    private String branchName;

    private Long totalAlerts;

    private Double averageRiskScore;

}