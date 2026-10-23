package com.zosh.payload.AdminAnalysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreDashboardStatsDTO {
    private Long total;
    private Long active;
    private Long pending;
    private Long blocked;
    private Long closed;
}
