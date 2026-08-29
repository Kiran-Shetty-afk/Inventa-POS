package com.zosh.pricing.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingPredictionRequest {

    private String category;

    private Double current_price;

    private Integer stock_remaining;

    private Integer weekly_sales;

    private String day;

    private String month;

    private Integer weekend;

}