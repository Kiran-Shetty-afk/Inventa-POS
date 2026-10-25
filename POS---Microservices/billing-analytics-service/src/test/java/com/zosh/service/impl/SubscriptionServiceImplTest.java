package com.zosh.service.impl;

import com.zosh.domain.PaymentStatus;
import com.zosh.domain.SubscriptionStatus;
import com.zosh.modal.Subscription;
import com.zosh.modal.SubscriptionPlan;
import com.zosh.repository.SubscriptionPlanRepository;
import com.zosh.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubscriptionServiceImplTest {

    @Test
    void newSubscriptionRemainsPendingAndCannotBecomeActiveBeforePayment() {
        SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
        SubscriptionPlanRepository plans = mock(SubscriptionPlanRepository.class);
        SubscriptionPlan plan = SubscriptionPlan.builder().id(4L).name("Test plan")
                .price(new BigDecimal("100.00")).build();
        when(plans.findById(4L)).thenReturn(Optional.of(plan));
        when(subscriptions.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subscription result = new SubscriptionServiceImpl(subscriptions, plans)
                .createSubscription(12L, "Test store", 4L, "RAZORPAY", "pending-reference");

        assertEquals(SubscriptionStatus.TRIAL, result.getStatus());
        assertEquals(PaymentStatus.PENDING, result.getPaymentStatus());
    }
}
