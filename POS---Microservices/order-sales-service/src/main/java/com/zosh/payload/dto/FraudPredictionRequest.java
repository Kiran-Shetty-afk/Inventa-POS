package com.zosh.payload.dto;

public record FraudPredictionRequest(
        double totalAmount,
        String paymentType,
        int itemsCount,
        int totalQuantity,
        double averageItemPrice,
        double discountPercent,
        int orderHour
) {
}
