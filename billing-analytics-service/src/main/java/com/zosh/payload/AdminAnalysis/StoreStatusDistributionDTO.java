package com.zosh.payload.AdminAnalysis;

import com.zosh.domain.StoreStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreStatusDistributionDTO {
    private StoreStatus status;
    private Long count;
}
