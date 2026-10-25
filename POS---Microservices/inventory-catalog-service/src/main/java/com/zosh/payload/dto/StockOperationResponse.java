package com.zosh.payload.dto;

import com.zosh.domain.StockOperationStatus;
import com.zosh.domain.StockOperationType;

public record StockOperationResponse(
        String operationKey,
        Long branchId,
        Long productId,
        Integer quantity,
        StockOperationType operationType,
        StockOperationStatus status) {
}
