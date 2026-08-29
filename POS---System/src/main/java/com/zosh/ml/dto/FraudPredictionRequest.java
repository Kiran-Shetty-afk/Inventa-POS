package com.zosh.ml.dto;

import lombok.Data;

@Data
public class FraudPredictionRequest {

    private Double totalAmount;
    private String paymentType;
    private Integer itemsCount;
    private Integer totalQuantity;
    private Double averageItemPrice;
    private Double discountPercent;
    private Integer orderHour;

}