package com.zosh.service;

import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentRefundStatus;
import com.zosh.modal.Order;
import com.zosh.modal.Refund;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.OrderWorkflowRepository;
import com.zosh.repository.OrderWorkflowStepRepository;
import com.zosh.repository.RefundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderWorkflowCoordinatorRefundTest {
    @Mock private OrderRepository orderRepository;
    @Mock private OrderWorkflowRepository workflowRepository;
    @Mock private OrderWorkflowStepRepository stepRepository;
    @Mock private RefundRepository refundRepository;
    @InjectMocks private OrderWorkflowCoordinator coordinator;

    @Test
    void stockRestorationCompletesOnlyTheStockPartAndLeavesPaymentForReview() {
        Order order = Order.builder().id(30L).status(OrderStatus.REFUNDING).build();
        Refund refund = Refund.builder().id(45L).order(order).processingStatus("PENDING")
                .paymentRefundStatus(PaymentRefundStatus.NOT_ISSUED).build();
        when(refundRepository.findById(45L)).thenReturn(Optional.of(refund));
        when(orderRepository.findById(30L)).thenReturn(Optional.of(order));

        coordinator.completeRefund(45L);
        coordinator.completeRefund(45L);

        assertEquals("COMPLETED", refund.getProcessingStatus());
        assertEquals(PaymentRefundStatus.MANUAL_REVIEW_REQUIRED, refund.getPaymentRefundStatus());
        assertEquals(OrderStatus.REFUND_PENDING_REVIEW, order.getStatus());
        verify(refundRepository, times(1)).save(refund);
        verify(orderRepository, times(1)).save(order);
    }
}
