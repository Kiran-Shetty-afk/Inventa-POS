package com.zosh.service;

import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentRefundStatus;
import com.zosh.modal.Order;
import com.zosh.modal.OrderItem;
import com.zosh.modal.Refund;
import com.zosh.payload.dto.RefundItemDTO;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.OrderWorkflowRepository;
import com.zosh.repository.OrderWorkflowStepRepository;
import com.zosh.repository.RefundItemRepository;
import com.zosh.repository.RefundRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefundAllocationTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderWorkflowRepository workflows = mock(OrderWorkflowRepository.class);
    private final OrderWorkflowStepRepository steps = mock(OrderWorkflowStepRepository.class);
    private final RefundRepository refunds = mock(RefundRepository.class);
    private final RefundItemRepository refundItems = mock(RefundItemRepository.class);
    private final LoyaltyPointsService loyalty = mock(LoyaltyPointsService.class);
    private final OrderWorkflowCoordinator coordinator = new OrderWorkflowCoordinator(
            orders, workflows, steps, refunds, refundItems, loyalty);

    @Test
    void partialQuantitiesUsePersistedDiscountAndTaxAndRepeatedRefundsDoNotExceedBoughtQuantity() {
        OrderItem first = item(11L, 101L, 3, "60.00");
        OrderItem second = item(12L, 202L, 2, "40.00");
        Order order = Order.builder().id(8L).status(OrderStatus.COMPLETED)
                .subtotalAmount(new BigDecimal("100.00")).discountAmount(new BigDecimal("10.00"))
                .taxAmount(new BigDecimal("5.00")).totalAmount(new BigDecimal("95.00"))
                .items(List.of(first, second)).build();
        when(orders.findByIdForUpdate(8L)).thenReturn(Optional.of(order));
        when(orders.findWithItemsAndCustomerById(8L)).thenReturn(Optional.of(order));
        when(refunds.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(refunds.findByOrderIdOrderByIdAsc(8L)).thenReturn(List.of());
        when(refunds.sumAmountByOrderIdAndPaymentRefundStatus(8L, PaymentRefundStatus.SUCCEEDED))
                .thenReturn(BigDecimal.ZERO);
        Map<Long, Integer> reserved = new HashMap<>();
        when(refundItems.sumReservedQuantityByOrderItemId(any())).thenAnswer(invocation ->
                reserved.getOrDefault(invocation.getArgument(0), 0));
        when(refunds.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Refund partial = coordinator.startPendingRefund(8L, candidate("one"),
                List.of(RefundItemDTO.builder().orderItemId(11L).quantity(2).build()));
        assertEquals(new BigDecimal("38.00"), partial.getAmount());
        assertEquals(2, partial.getItems().get(0).getQuantity());
        reserved.put(11L, 2);
        order.setStatus(OrderStatus.PARTIALLY_REFUNDED);
        when(refunds.findByOrderIdOrderByIdAsc(8L)).thenReturn(List.of(partial));
        when(refunds.sumAmountByOrderIdAndPaymentRefundStatus(8L, PaymentRefundStatus.SUCCEEDED))
                .thenReturn(new BigDecimal("38.00"));

        Refund next = coordinator.startPendingRefund(8L, candidate("two"),
                List.of(RefundItemDTO.builder().orderItemId(11L).quantity(1).build()));
        assertEquals(new BigDecimal("19.00"), next.getAmount());
        reserved.put(11L, 3);
        order.setStatus(OrderStatus.PARTIALLY_REFUNDED);

        assertThrows(IllegalArgumentException.class, () -> coordinator.startPendingRefund(8L,
                candidate("three"), List.of(RefundItemDTO.builder().orderItemId(11L).quantity(1).build())));
        verify(refunds, times(2)).saveAndFlush(any());
    }

    private Refund candidate(String key) {
        return Refund.builder().idempotencyKey("refund:" + key).items(new ArrayList<>()).build();
    }
    private OrderItem item(Long id, Long productId, int quantity, String price) {
        return OrderItem.builder().id(id).productId(productId).productName("Item " + id)
                .quantity(quantity).price(new BigDecimal(price))
                .productPrice(new BigDecimal("20.00")).build();
    }
}
