ALTER TABLE `refunds`
    ADD COLUMN `payment_refund_confirmed_at` DATETIME(6) NULL,
    ADD COLUMN `payment_refund_confirmed_by` BIGINT NULL;
