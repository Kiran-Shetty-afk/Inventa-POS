package com.zosh.payload.dto;

import com.zosh.domain.StockOperationType;

public record StockOperationRequest(
        String operationKey,
        Long branchId,
        Long productId,
        Integer quantity,
        StockOperationType operationType) {
}
