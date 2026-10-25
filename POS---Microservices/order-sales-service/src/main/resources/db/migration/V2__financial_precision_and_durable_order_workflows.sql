ALTER TABLE `order_items`
    MODIFY COLUMN `price` DECIMAL(19,2) NULL,
    MODIFY COLUMN `product_price` DECIMAL(19,2) NULL;

ALTER TABLE `orders`
    ADD COLUMN `idempotency_key` VARCHAR(64) NULL,
    ADD COLUMN `subtotal_amount` DECIMAL(19,2) NULL,
    ADD COLUMN `tax_rate` DECIMAL(7,4) NULL,
    ADD COLUMN `tax_amount` DECIMAL(19,2) NULL,
    MODIFY COLUMN `average_item_price` DECIMAL(19,6) NULL,
    MODIFY COLUMN `discount_amount` DECIMAL(19,2) NULL,
    MODIFY COLUMN `total_amount` DECIMAL(19,2) NULL,
    MODIFY COLUMN `status` ENUM('CANCELLED','CANCELLING','COMPLETED','FAILED','PENDING','REFUNDED','REFUNDING','REFUND_FAILED') NULL;

CREATE UNIQUE INDEX `uk_orders_idempotency_key` ON `orders` (`idempotency_key`);

ALTER TABLE `refunds`
    MODIFY COLUMN `amount` DECIMAL(19,2) NULL,
    ADD COLUMN `processing_status` VARCHAR(16) NOT NULL DEFAULT 'COMPLETED';

ALTER TABLE `shift_reports`
    MODIFY COLUMN `card_sales` DECIMAL(19,2) NULL,
    MODIFY COLUMN `cash_sales` DECIMAL(19,2) NULL,
    MODIFY COLUMN `net_sales` DECIMAL(19,2) NULL,
    MODIFY COLUMN `total_refunds` DECIMAL(19,2) NULL,
    MODIFY COLUMN `total_sales` DECIMAL(19,2) NULL,
    MODIFY COLUMN `upi_sales` DECIMAL(19,2) NULL;

CREATE TABLE `order_workflows` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL,
    `branch_id` BIGINT NOT NULL,
    `state` VARCHAR(20) NOT NULL,
    `failure_reason` VARCHAR(300) NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `version` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_order_workflow_order` UNIQUE (`order_id`),
    CONSTRAINT `fk_order_workflow_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
    KEY `idx_order_workflow_state_updated` (`state`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `order_workflow_steps` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `workflow_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL,
    `reservation_operation_key` VARCHAR(128) NOT NULL,
    `restoration_operation_key` VARCHAR(128) NOT NULL,
    `reservation_state` VARCHAR(16) NOT NULL,
    `restoration_state` VARCHAR(16) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_order_workflow_product` UNIQUE (`workflow_id`, `product_id`),
    CONSTRAINT `uk_order_reservation_operation` UNIQUE (`reservation_operation_key`),
    CONSTRAINT `uk_order_restoration_operation` UNIQUE (`restoration_operation_key`),
    CONSTRAINT `fk_order_workflow_step_workflow` FOREIGN KEY (`workflow_id`)
        REFERENCES `order_workflows` (`id`) ON DELETE CASCADE,
    KEY `idx_order_workflow_step_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
