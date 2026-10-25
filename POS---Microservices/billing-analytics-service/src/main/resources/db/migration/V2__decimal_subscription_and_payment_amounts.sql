ALTER TABLE `payment_orders`
    MODIFY COLUMN `amount` DECIMAL(19,2) NULL;

ALTER TABLE `subscription_plans`
    MODIFY COLUMN `price` DECIMAL(19,2) NOT NULL;
