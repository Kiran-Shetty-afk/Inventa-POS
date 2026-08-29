package com.zosh.pricing.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRecommendationDTO {

    private Long productId;

    private String productName;

    private Double currentPrice;

    private Double recommendedPrice;

    private Integer stockRemaining;

    private String demand;

    private String reason;

    private Double expectedRevenueIncrease;

}