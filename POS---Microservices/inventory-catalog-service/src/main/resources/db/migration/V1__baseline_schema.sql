-- Baseline for the schema inspected on the existing local MySQL 8.4 instance.
-- Fresh databases execute this once; existing schemas must be compared and baselined manually.
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `inventories` (
  `id` bigint NOT NULL,
  `branch_id` bigint NOT NULL,
  `last_updated` datetime(6) DEFAULT NULL,
  `quantity` int NOT NULL,
  `product_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inventory_branch_product` (`branch_id`,`product_id`),
  KEY `FK8drmqyx629j3oo8ct9jnc5y3y` (`product_id`),
  CONSTRAINT `FK8drmqyx629j3oo8ct9jnc5y3y` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `inventories_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `products` (
  `id` bigint NOT NULL,
  `brand` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `image` varchar(255) DEFAULT NULL,
  `mrp` double NOT NULL,
  `name` varchar(255) NOT NULL,
  `selling_price` double NOT NULL,
  `sku` varchar(255) NOT NULL,
  `store_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKfhmd06dsmj6k0n90swsh8ie9g` (`sku`),
  KEY `FKog2rp4qthbtt2lfyhfo32lsw9` (`category_id`),
  CONSTRAINT `FKog2rp4qthbtt2lfyhfo32lsw9` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `products_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


SET FOREIGN_KEY_CHECKS = 1;
