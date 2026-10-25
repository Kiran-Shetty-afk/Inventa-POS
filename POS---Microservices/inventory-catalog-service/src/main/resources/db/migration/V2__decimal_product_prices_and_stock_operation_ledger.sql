ALTER TABLE `products`
    MODIFY COLUMN `mrp` DECIMAL(19,2) NOT NULL,
    MODIFY COLUMN `selling_price` DECIMAL(19,2) NOT NULL;

CREATE TABLE `stock_operations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `operation_key` VARCHAR(128) NOT NULL,
    `branch_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL,
    `operation_type` VARCHAR(16) NOT NULL,
    `status` VARCHAR(16) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_stock_operation_key` UNIQUE (`operation_key`),
    KEY `idx_stock_operation_branch_product` (`branch_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
