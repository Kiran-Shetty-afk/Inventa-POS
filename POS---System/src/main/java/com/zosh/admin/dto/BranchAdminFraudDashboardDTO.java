package com.zosh.admin.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchAdminFraudDashboardDTO {

    private Long totalAlerts;

    private Long criticalAlerts;

    private Long resolvedAlerts;

    private Double averageRiskScore;

    private Long totalBranches;

    private String highestRiskBranch;
    private List<BranchFraudAnalyticsDTO> branchAnalytics;

}