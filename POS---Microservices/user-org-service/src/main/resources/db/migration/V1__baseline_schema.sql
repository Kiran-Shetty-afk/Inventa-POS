-- Baseline for the schema inspected on the existing local MySQL 8.4 instance.
-- Fresh databases execute this once; existing schemas must be compared and baselined manually.
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `branch_working_days` (
  `branch_id` bigint NOT NULL,
  `working_days` varchar(255) DEFAULT NULL,
  KEY `FK1pw0iposltokqnwyygxhix9oy` (`branch_id`),
  CONSTRAINT `FK1pw0iposltokqnwyygxhix9oy` FOREIGN KEY (`branch_id`) REFERENCES `branches` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `branches` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL,
  `close_time` time(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `open_time` time(6) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `manager_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9sgoag24qgk3nabu6g7416ijm` (`manager_id`),
  KEY `FKc2bywqbckf4tnouya6o5q9rbg` (`store_id`),
  CONSTRAINT `FKaxphe54ft6x2k2ndo8t5vsvjo` FOREIGN KEY (`manager_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKc2bywqbckf4tnouya6o5q9rbg` FOREIGN KEY (`store_id`) REFERENCES `stores` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `password_reset_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expiry_date` datetime(6) NOT NULL,
  `token` varchar(255) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK71lqwbwtklmljk3qlsugr1mig` (`token`),
  KEY `FKk3ndxg5xp6v7wd4gjyusp15gq` (`user_id`),
  CONSTRAINT `FKk3ndxg5xp6v7wd4gjyusp15gq` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `stores` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `brand` varchar(255) NOT NULL,
  `address` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `status` enum('ACTIVE','CLOSED','PENDING','SUSPENDED') DEFAULT NULL,
  `store_type` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `store_admin_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKb1981d6rapqjv3hdqbg0h03l2` (`store_admin_id`),
  CONSTRAINT `FKn34o6e266esj3ickq4ecol23k` FOREIGN KEY (`store_admin_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `last_login` datetime(6) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `role` tinyint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `verified` bit(1) NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  `store_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  KEY `FK9o70sp9ku40077y38fk4wieyk` (`branch_id`),
  KEY `FK7wra86jadsraitoewujbjj1pd` (`store_id`),
  CONSTRAINT `FK7wra86jadsraitoewujbjj1pd` FOREIGN KEY (`store_id`) REFERENCES `stores` (`id`),
  CONSTRAINT `FK9o70sp9ku40077y38fk4wieyk` FOREIGN KEY (`branch_id`) REFERENCES `branches` (`id`),
  CONSTRAINT `users_chk_1` CHECK ((`role` between 0 and 7))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


SET FOREIGN_KEY_CHECKS = 1;
