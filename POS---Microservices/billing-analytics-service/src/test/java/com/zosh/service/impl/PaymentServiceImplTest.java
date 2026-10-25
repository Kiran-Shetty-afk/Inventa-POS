package com.zosh.service.impl;

import com.zosh.domain.PaymentMethod;
import com.zosh.repository.PaymentOrderRepository;
import com.zosh.repository.SubscriptionPlanRepository;
import com.zosh.service.SubscriptionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class PaymentServiceImplTest {

    @Test
    void stripeIsRejectedBeforeCreatingAnOrderUntilWebhookReconciliationExists() {
        PaymentOrderRepository paymentOrders = mock(PaymentOrderRepository.class);
        SubscriptionPlanRepository plans = mock(SubscriptionPlanRepository.class);
        SubscriptionService subscriptions = mock(SubscriptionService.class);
        PaymentServiceImpl service = new PaymentServiceImpl(paymentOrders, plans, subscriptions);

        assertThrows(IllegalStateException.class, () -> service.createOrder(
                1L, "user@example.invalid", "Test User", 2L, 3L, PaymentMethod.STRIPE));
        verifyNoInteractions(paymentOrders, plans, subscriptions);
    }
}
