package com.zosh.domain;

/** Financial refund state, kept separate from inventory restoration state. */
public enum PaymentRefundStatus {
    NOT_ISSUED,
    MANUAL_REVIEW_REQUIRED,
    PENDING_PROVIDER,
    SUCCEEDED,
    FAILED
}
