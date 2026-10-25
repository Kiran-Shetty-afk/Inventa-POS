INSERT INTO `products_seq` (`next_val`)
SELECT COALESCE((SELECT MAX(`id`) FROM `products`), 0) + 1
WHERE NOT EXISTS (SELECT 1 FROM `products_seq`);
