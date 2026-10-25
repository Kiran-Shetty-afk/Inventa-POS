-- Baseline for the schema inspected on the existing local MySQL 8.4 instance.
-- Fresh databases execute this once; existing schemas must be compared and baselined manually.
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `payment_orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` double DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `payment_link_id` varchar(255) DEFAULT NULL,
  `payment_method` enum('RAZORPAY','STRIPE') DEFAULT NULL,
  `plan_id` bigint NOT NULL,
  `status` enum('FAILED','PENDING','SUCCESS') DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  `user_email` varchar(255) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `subscription_plan_extra_features` (
  `subscription_plan_id` bigint NOT NULL,
  `extra_features` varchar(255) DEFAULT NULL,
  KEY `FK57npp0n5umwb6fqkcfbdiojvn` (`subscription_plan_id`),
  CONSTRAINT `FK57npp0n5umwb6fqkcfbdiojvn` FOREIGN KEY (`subscription_plan_id`) REFERENCES `subscription_plans` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `subscription_plans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `billing_cycle` enum('ANNUAL','CUSTOM','MONTHLY','YEARLY') NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) NOT NULL,
  `enable_advanced_reports` bit(1) DEFAULT NULL,
  `enable_ecommerce` bit(1) DEFAULT NULL,
  `enable_integrations` bit(1) DEFAULT NULL,
  `enable_inventory` bit(1) DEFAULT NULL,
  `enable_invoice_branding` bit(1) DEFAULT NULL,
  `enable_multi_location` bit(1) DEFAULT NULL,
  `max_branches` int NOT NULL,
  `max_products` int NOT NULL,
  `max_users` int NOT NULL,
  `name` varchar(255) NOT NULL,
  `price` double NOT NULL,
  `priority_support` bit(1) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `active` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `subscriptions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `end_date` date NOT NULL,
  `payment_gateway` varchar(255) DEFAULT NULL,
  `payment_status` enum('FAILED','PAID','PENDING') NOT NULL,
  `start_date` date NOT NULL,
  `status` enum('ACTIVE','CANCELLED','EXPIRED','TRIAL') NOT NULL,
  `store_id` bigint NOT NULL,
  `store_name` varchar(255) DEFAULT NULL,
  `transaction_id` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `plan_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKg41f5iev0mretaqhvepf0lks0` (`plan_id`),
  CONSTRAINT `FKg41f5iev0mretaqhvepf0lks0` FOREIGN KEY (`plan_id`) REFERENCES `subscription_plans` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


SET FOREIGN_KEY_CHECKS = 1;
