package com.zosh.payload.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDemandForecastDTO {
    private Long productId;
    private String productName;
    private Integer currentStock;
    private double forecast7;
    private double forecast14;
    private double forecast30;
    private Integer recommendedReorderQty;
    private Integer recommendedHorizonDays;
    private boolean reorderSuggested;
    private String basis;
}
