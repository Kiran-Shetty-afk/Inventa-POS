package com.zosh.payload.dto;

import java.math.BigDecimal;

public record BranchSummaryDTO(Long id, Long storeId, String name, BigDecimal maxDiscountPercent) {
    public BranchSummaryDTO(Long id, Long storeId) {
        this(id, storeId, null, BigDecimal.ZERO.setScale(2));
    }

    public BranchSummaryDTO(Long id, Long storeId, String name) {
        this(id, storeId, name, BigDecimal.ZERO.setScale(2));
    }
}
