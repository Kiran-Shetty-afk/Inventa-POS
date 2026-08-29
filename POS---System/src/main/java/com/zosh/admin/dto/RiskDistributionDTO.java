package com.zosh.admin.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskDistributionDTO {

    private String riskLevel;
    private Long total;

}