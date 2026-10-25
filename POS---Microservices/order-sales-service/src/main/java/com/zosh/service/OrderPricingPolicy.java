package com.zosh.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Server-owned checkout math. Tax values remain business configuration, not a legal determination. */
@Component
public class OrderPricingPolicy {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal MAX_BRANCH_DISCOUNT_PERCENT = new BigDecimal("15.00");

    private final BigDecimal taxRatePercent;

    public OrderPricingPolicy(
            @Value("${orders.tax.rate-percent:18}") BigDecimal taxRatePercent) {
        this.taxRatePercent = validatePercent(taxRatePercent, "Tax rate");
    }

    public BigDecimal taxRatePercent() {
        return taxRatePercent.setScale(4, RoundingMode.UNNECESSARY);
    }

    /** Missing branch configuration fails closed to zero; invalid persisted values fail closed as well. */
    public BigDecimal validatedBranchDiscountLimit(BigDecimal configuredLimit) {
        BigDecimal limit = configuredLimit == null ? BigDecimal.ZERO : configuredLimit;
        if (limit.signum() < 0 || limit.compareTo(MAX_BRANCH_DISCOUNT_PERCENT) > 0
                || limit.stripTrailingZeros().scale() > 2) {
            throw new IllegalStateException("The branch discount policy is outside the allowed 0–15% range");
        }
        return limit.setScale(2, RoundingMode.UNNECESSARY);
    }

    public BigDecimal calculateDiscountAmount(
            BigDecimal subtotal, BigDecimal requestedPercent, BigDecimal configuredBranchLimit) {
        requireNonNegative(subtotal, "Subtotal");
        BigDecimal percent = validateDiscountPercent(requestedPercent);
        BigDecimal branchLimit = validatedBranchDiscountLimit(configuredBranchLimit);
        if (percent.compareTo(branchLimit) > 0) {
            throw new IllegalArgumentException("Discount percentage exceeds this branch's configured limit");
        }
        return subtotal.multiply(percent).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTax(BigDecimal subtotal) {
        requireNonNegative(subtotal, "Subtotal");
        return subtotal.multiply(taxRatePercent)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal validateDiscountPercent(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(MAX_BRANCH_DISCOUNT_PERCENT) > 0
                || value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Discount percentage must be between 0% and 15% with at most two decimal places");
        }
        return value;
    }

    private static BigDecimal validatePercent(BigDecimal value, String name) {
        if (value == null || value.signum() < 0 || value.compareTo(ONE_HUNDRED) > 0
                || value.stripTrailingZeros().scale() > 4) {
            throw new IllegalArgumentException(name + " must be between 0 and 100 with at most four decimal places");
        }
        return value;
    }

    private static void requireNonNegative(BigDecimal value, String name) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
