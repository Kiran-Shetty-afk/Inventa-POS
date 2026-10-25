package com.zosh.domain;

/**
 * Records what the POS itself knows about an order payment. This deliberately
 * has no provider-confirmed state because order checkout has no trusted card/UPI
 * settlement callback today.
 */
public enum PaymentSettlementStatus {
    CASH_RECORDED,
    EXTERNAL_CONFIRMATION_REQUIRED
}
