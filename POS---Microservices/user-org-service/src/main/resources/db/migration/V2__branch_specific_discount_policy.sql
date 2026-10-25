ALTER TABLE `branches`
    ADD COLUMN `max_discount_percent` DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    ADD CONSTRAINT `chk_branches_max_discount_percent`
        CHECK (`max_discount_percent` >= 0.00 AND `max_discount_percent` <= 15.00);
