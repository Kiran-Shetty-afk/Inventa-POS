package com.zosh.service;

import com.zosh.modal.Customer;
import com.zosh.modal.Order;
import com.zosh.modal.Refund;
import com.zosh.repository.CustomerLoyaltyTransactionRepository;
import com.zosh.repository.CustomerRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoyaltyPointsServiceTest {
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final CustomerLoyaltyTransactionRepository ledger = mock(CustomerLoyaltyTransactionRepository.class);
    private final LoyaltyPointsService service = new LoyaltyPointsService(customers, ledger);

    @Test
    void confirmedPartialRefundClawsBackOnlyEarnedFractionAndRecordsDebtWhenPointsWereSpent() {
        Customer customer = Customer.builder().id(4L).loyaltyPoints(2).loyaltyPointsDebt(0).build();
        Order order = Order.builder().id(8L).customer(customer).totalAmount(new BigDecimal("1000.00")).build();
        Refund refund = Refund.builder().id(12L).order(order).build();
        when(ledger.findByIdempotencyKey("loyalty:refund-reversal:12")).thenReturn(Optional.empty());
        when(ledger.sumReversedPointsByOrderId(8L)).thenReturn(0);
        when(customers.findByIdForUpdate(4L)).thenReturn(Optional.of(customer));

        service.reverseForConfirmedRefund(order, refund, new BigDecimal("300.00"));

        assertEquals(0, customer.getLoyaltyPoints());
        assertEquals(1, customer.getLoyaltyPointsDebt());
        verify(ledger).save(argThat(entry -> entry.getReversedPoints() == 3
                && entry.getPointsDelta() == -2 && entry.getDebtDelta() == 1));
    }

    @Test
    void orderPointsAreCreditedOnceAndFirstRepayExistingDebt() {
        Customer customer = Customer.builder().id(4L).loyaltyPoints(10).loyaltyPointsDebt(7).build();
        Order order = Order.builder().id(8L).customer(customer).totalAmount(new BigDecimal("1000.00")).build();
        when(ledger.findByIdempotencyKey("loyalty:order-earned:8"))
                .thenReturn(Optional.empty(), Optional.of(com.zosh.modal.CustomerLoyaltyTransaction.builder().build()));
        when(customers.findByIdForUpdate(4L)).thenReturn(Optional.of(customer));

        service.creditForOrder(order);
        service.creditForOrder(order);

        assertEquals(13, customer.getLoyaltyPoints());
        assertEquals(0, customer.getLoyaltyPointsDebt());
        verify(customers, times(1)).save(customer);
        verify(ledger, times(1)).save(any());
    }
}
