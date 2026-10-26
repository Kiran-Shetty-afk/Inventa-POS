package com.zosh.payload.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LoyaltyTransactionDTO(
        Long id, Long customerId, Long orderId, Long refundId, String entryType,
        Integer pointsDelta, Integer debtDelta, Integer reversedPoints,
        Integer balanceAfter, Integer debtAfter, LocalDateTime createdAt) {}
