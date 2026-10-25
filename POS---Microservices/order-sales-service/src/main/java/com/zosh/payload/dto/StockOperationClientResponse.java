package com.zosh.payload.dto;

public record StockOperationClientResponse(
        String operationKey,
        Long branchId,
        Long productId,
        Integer quantity,
        String operationType,
        String status) {
}
