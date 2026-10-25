package com.zosh.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderPricingPolicyTest {

    @Test
    void keepsLegacyEighteenPercentTaxOnPreDiscountSubtotalByDefault() {
        OrderPricingPolicy policy = new OrderPricingPolicy(new BigDecimal("18"));

        assertEquals(new BigDecimal("18.0000"), policy.taxRatePercent());
        assertEquals(new BigDecimal("0.03"), policy.calculateTax(new BigDecimal("0.15")));
    }

    @Test
    void taxIsCalculatedOnTheOriginalSubtotalBeforeDiscount() {
        OrderPricingPolicy policy = new OrderPricingPolicy(new BigDecimal("18"));
        BigDecimal subtotal = new BigDecimal("100.00");
        BigDecimal discount = new BigDecimal("10.00");
        BigDecimal tax = policy.calculateTax(subtotal);

        assertEquals(new BigDecimal("18.00"), tax);
        assertEquals(new BigDecimal("108.00"), subtotal.subtract(discount).add(tax));
    }

    @Test
    void supportsZeroTaxAndRejectsInvalidConfiguredRates() {
        OrderPricingPolicy zeroTax = new OrderPricingPolicy(BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), zeroTax.calculateTax(new BigDecimal("12.34")));
        assertThrows(IllegalArgumentException.class,
                () -> new OrderPricingPolicy(new BigDecimal("100.0001")));
    }

    @Test
    void calculatesBranchSpecificDiscountsAtTheCurrencyBoundary() {
        OrderPricingPolicy policy = new OrderPricingPolicy(new BigDecimal("18"));
        BigDecimal subtotal = new BigDecimal("100.00");
        for (String percent : new String[] {"0", "5", "10", "15"}) {
            assertEquals(new BigDecimal(percent).setScale(2),
                    policy.calculateDiscountAmount(subtotal, new BigDecimal(percent), new BigDecimal("15.00")));
        }
        assertThrows(IllegalArgumentException.class,
                () -> policy.calculateDiscountAmount(subtotal, new BigDecimal("16"), new BigDecimal("15")));
        assertThrows(IllegalArgumentException.class,
                () -> policy.calculateDiscountAmount(subtotal, new BigDecimal("-0.01"), new BigDecimal("15")));
        assertThrows(IllegalArgumentException.class,
                () -> policy.calculateDiscountAmount(subtotal, new BigDecimal("1.001"), new BigDecimal("15")));
        assertThrows(IllegalStateException.class,
                () -> policy.calculateDiscountAmount(subtotal, BigDecimal.ZERO, new BigDecimal("16")));
        assertEquals(new BigDecimal("0.01"),
                policy.calculateDiscountAmount(new BigDecimal("0.05"), new BigDecimal("15"), new BigDecimal("15")));
    }
}
