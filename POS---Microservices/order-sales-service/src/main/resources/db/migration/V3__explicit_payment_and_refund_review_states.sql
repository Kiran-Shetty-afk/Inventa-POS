-- Separate an in-store method record from externally verified settlement.
ALTER TABLE `orders`
    ADD COLUMN `payment_settlement_status` VARCHAR(40) NOT NULL
        DEFAULT 'EXTERNAL_CONFIRMATION_REQUIRED';

UPDATE `orders`
SET `payment_settlement_status` = CASE
    WHEN `payment_type` = 'CASH' THEN 'CASH_RECORDED'
    ELSE 'EXTERNAL_CONFIRMATION_REQUIRED'
END;

-- Stock restoration did not execute or confirm a financial refund. Reclassify
-- legacy REFUNDED rows as awaiting financial review instead of claiming success.
ALTER TABLE `orders`
    MODIFY COLUMN `status` ENUM(
        'CANCELLED','CANCELLING','COMPLETED','FAILED','PENDING','REFUNDED',
        'REFUNDING','REFUND_PENDING_REVIEW','REFUND_FAILED'
    ) DEFAULT NULL;

UPDATE `orders`
SET `status` = 'REFUND_PENDING_REVIEW'
WHERE `status` = 'REFUNDED';

-- processing_status continues to mean inventory restoration for API compatibility.
ALTER TABLE `refunds`
    ADD COLUMN `idempotency_key` VARCHAR(64) NULL,
    ADD COLUMN `payment_refund_status` VARCHAR(32) NOT NULL DEFAULT 'NOT_ISSUED';

UPDATE `refunds`
SET `payment_refund_status` = 'MANUAL_REVIEW_REQUIRED'
WHERE `processing_status` = 'COMPLETED';

CREATE UNIQUE INDEX `uk_refunds_idempotency_key`
    ON `refunds` (`idempotency_key`);
