package com.zosh.domain;

public enum OrderStatus {
    COMPLETED,
    PENDING,
    CANCELLING,
    REFUNDED,
    REFUNDING,
    REFUND_PENDING_REVIEW,
    PARTIALLY_REFUNDED,
    REFUND_FAILED,
    FAILED,
    CANCELLED
}
