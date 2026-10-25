-- Hibernate's MySQL table generator needs an initialized high-value row.
-- Preserve existing IDs by starting after the current maximum.
INSERT INTO `inventories_seq` (`next_val`)
SELECT COALESCE((SELECT MAX(`id`) FROM `inventories`), 0) + 1
WHERE NOT EXISTS (SELECT 1 FROM `inventories_seq`);

UPDATE `inventories_seq`
SET `next_val` = GREATEST(
    COALESCE(`next_val`, 1),
    COALESCE((SELECT MAX(`id`) + 1 FROM `inventories`), 1)
)
WHERE `next_val` IS NULL
   OR `next_val` < COALESCE((SELECT MAX(`id`) + 1 FROM `inventories`), 1);
