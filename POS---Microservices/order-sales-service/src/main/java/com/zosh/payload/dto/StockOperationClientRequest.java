package com.zosh.payload.dto;

public record StockOperationClientRequest(
        String operationKey,
        Long branchId,
        Long productId,
        Integer quantity,
        String operationType) {
}
