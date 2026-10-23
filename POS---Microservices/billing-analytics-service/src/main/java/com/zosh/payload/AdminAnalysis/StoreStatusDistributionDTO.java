package com.zosh.payload.AdminAnalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreStatusDistributionDTO {
    private Long total;
    private Long active;
    private Long pending;
    private Long blocked;
    private Long closed;
}
