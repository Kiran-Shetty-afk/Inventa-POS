-- Baseline for the schema inspected on the existing local MySQL 8.4 instance.
-- Fresh databases execute this once; existing schemas must be compared and baselined manually.
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `customer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `full_name` varchar(255) NOT NULL,
  `loyalty_points` int NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `order_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `price` double DEFAULT NULL,
  `product_id` bigint NOT NULL,
  `product_image` varchar(255) DEFAULT NULL,
  `product_name` varchar(255) DEFAULT NULL,
  `product_price` double DEFAULT NULL,
  `product_sku` varchar(255) DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `order_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbioxgbv59vetrxe0ejfubep1w` (`order_id`),
  CONSTRAINT `FKbioxgbv59vetrxe0ejfubep1w` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `average_item_price` double DEFAULT NULL,
  `branch_id` bigint DEFAULT NULL,
  `cashier_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `day_of_week` varchar(255) DEFAULT NULL,
  `discount_amount` double DEFAULT NULL,
  `discount_percent` double DEFAULT NULL,
  `is_fraud` bit(1) DEFAULT NULL,
  `items_count` int DEFAULT NULL,
  `order_hour` int DEFAULT NULL,
  `payment_type` enum('CARD','CASH','UPI') DEFAULT NULL,
  `risk_score` double DEFAULT NULL,
  `score` int NOT NULL,
  `shift` varchar(255) DEFAULT NULL,
  `status` enum('CANCELLED','COMPLETED','PENDING','REFUNDED') DEFAULT NULL,
  `total_amount` double DEFAULT NULL,
  `total_quantity` int DEFAULT NULL,
  `customer_id` bigint DEFAULT NULL,
  `fraud_reason` varchar(500) DEFAULT NULL,
  `fraud_status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK624gtjin3po807j3vix093tlf` (`customer_id`),
  CONSTRAINT `FK624gtjin3po807j3vix093tlf` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `refunds` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` double DEFAULT NULL,
  `branch_id` bigint DEFAULT NULL,
  `cashier_id` bigint DEFAULT NULL,
  `cashier_name` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `payment_type` enum('CARD','CASH','UPI') DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `order_id` bigint DEFAULT NULL,
  `shift_report_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKsk9rqm7f6y8b1g0qob018hdm7` (`order_id`),
  KEY `FK4dps8qtfhr1s4l8x5yxalprbx` (`shift_report_id`),
  CONSTRAINT `FK4dps8qtfhr1s4l8x5yxalprbx` FOREIGN KEY (`shift_report_id`) REFERENCES `shift_reports` (`id`),
  CONSTRAINT `FKsk9rqm7f6y8b1g0qob018hdm7` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `shift_reports` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint DEFAULT NULL,
  `branch_name` varchar(255) DEFAULT NULL,
  `card_sales` double DEFAULT NULL,
  `cash_sales` double DEFAULT NULL,
  `cashier_id` bigint DEFAULT NULL,
  `cashier_name` varchar(255) DEFAULT NULL,
  `net_sales` double DEFAULT NULL,
  `shift_end` datetime(6) DEFAULT NULL,
  `shift_start` datetime(6) DEFAULT NULL,
  `total_orders` int NOT NULL,
  `total_refunds` double DEFAULT NULL,
  `total_sales` double DEFAULT NULL,
  `upi_sales` double DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


SET FOREIGN_KEY_CHECKS = 1;
