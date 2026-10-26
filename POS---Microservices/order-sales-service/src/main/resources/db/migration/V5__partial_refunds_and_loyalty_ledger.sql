ALTER TABLE `orders`
    MODIFY COLUMN `status` ENUM(
        'CANCELLED','CANCELLING','COMPLETED','FAILED','PENDING','PARTIALLY_REFUNDED',
        'REFUNDED','REFUNDING','REFUND_PENDING_REVIEW','REFUND_FAILED'
    ) DEFAULT NULL;

CREATE TABLE `refund_items` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `refund_id` BIGINT NOT NULL,
    `order_item_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL,
    `amount` DECIMAL(19,2) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_refund_item_refund_order_item` UNIQUE (`refund_id`, `order_item_id`),
    CONSTRAINT `fk_refund_item_refund` FOREIGN KEY (`refund_id`) REFERENCES `refunds` (`id`),
    CONSTRAINT `fk_refund_item_order_item` FOREIGN KEY (`order_item_id`) REFERENCES `order_items` (`id`),
    KEY `idx_refund_item_order_item` (`order_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `customer`
    ADD COLUMN `loyalty_points_debt` INT NOT NULL DEFAULT 0;

CREATE TABLE `customer_loyalty_transactions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `customer_id` BIGINT NOT NULL,
    `order_id` BIGINT NULL,
    `refund_id` BIGINT NULL,
    `entry_type` VARCHAR(32) NOT NULL,
    `points_delta` INT NOT NULL,
    `debt_delta` INT NOT NULL DEFAULT 0,
    `reversed_points` INT NOT NULL DEFAULT 0,
    `balance_after` INT NOT NULL,
    `debt_after` INT NOT NULL,
    `idempotency_key` VARCHAR(128) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_customer_loyalty_idempotency` UNIQUE (`idempotency_key`),
    CONSTRAINT `fk_customer_loyalty_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`),
    CONSTRAINT `fk_customer_loyalty_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
    CONSTRAINT `fk_customer_loyalty_refund` FOREIGN KEY (`refund_id`) REFERENCES `refunds` (`id`),
    KEY `idx_customer_loyalty_order` (`customer_id`, `order_id`),
    KEY `idx_customer_loyalty_refund` (`customer_id`, `refund_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `customer_loyalty_transactions`
    (`customer_id`, `entry_type`, `points_delta`, `debt_delta`, `reversed_points`,
     `balance_after`, `debt_after`, `idempotency_key`, `created_at`)
SELECT `id`, 'OPENING_BALANCE', `loyalty_points`, 0, 0,
       `loyalty_points`, 0, CONCAT('loyalty:opening:', `id`), CURRENT_TIMESTAMP(6)
FROM `customer`
WHERE `loyalty_points` > 0;
