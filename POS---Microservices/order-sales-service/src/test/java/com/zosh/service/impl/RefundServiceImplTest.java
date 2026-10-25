package com.zosh.service.impl;

import com.zosh.client.InventoryClient;
import com.zosh.client.UserOrgClient;
import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentRefundStatus;
import com.zosh.domain.PaymentType;
import com.zosh.modal.Order;
import com.zosh.modal.OrderItem;
import com.zosh.modal.Refund;
import com.zosh.payload.dto.CurrentUserDTO;
import com.zosh.payload.dto.RefundDTO;
import com.zosh.payload.dto.StockOperationClientRequest;
import com.zosh.payload.dto.StockOperationClientResponse;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.RefundRepository;
import com.zosh.repository.ShiftReportRepository;
import com.zosh.service.OrderWorkflowCoordinator;
import com.zosh.service.ServiceTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {
    @Mock private RefundRepository refundRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ShiftReportRepository shiftReportRepository;
    @Mock private InventoryClient inventoryClient;
    @Mock private ServiceTokenProvider tokenProvider;
    @Mock private UserOrgClient userOrgClient;
    @Mock private OrderWorkflowCoordinator coordinator;
    @InjectMocks private RefundServiceImpl service;

    @Test
    void uncertainRefundRestoreCanRetryWithTheSameOperationKey() {
        Refund pending = refundWithItems(
                item(7L, 2), item(9L, 1));
        Refund completed = refundWithItems(item(7L, 2), item(9L, 1));
        completed.setId(20L);
        completed.setProcessingStatus("COMPLETED");
        when(coordinator.getRefundForProcessing(20L)).thenReturn(pending, pending, completed);
        when(tokenProvider.authorizationHeader()).thenReturn("Bearer internal");
        when(inventoryClient.applyStockOperation(any(), eq("Bearer internal")))
                .thenThrow(new IllegalStateException("simulated timeout"))
                .thenReturn(response("refund:20:restore:7", 7L, 2))
                .thenReturn(response("refund:20:restore:9", 9L, 1));

        assertThrows(ResponseStatusException.class, () -> service.processRefund(20L));
        service.processRefund(20L);

        ArgumentCaptor<StockOperationClientRequest> requests =
                ArgumentCaptor.forClass(StockOperationClientRequest.class);
        verify(inventoryClient, org.mockito.Mockito.times(3))
                .applyStockOperation(requests.capture(), eq("Bearer internal"));
        org.junit.jupiter.api.Assertions.assertEquals(
                requests.getAllValues().get(0).operationKey(), requests.getAllValues().get(1).operationKey());
        verify(coordinator).completeRefund(20L);
        verify(coordinator, never()).failRefund(20L);
    }

    @Test
    void rejectedRestoreLeavesRefundVisibleForManualReconciliation() {
        Refund pending = refundWithItems(item(7L, 1));
        pending.setId(21L);
        when(coordinator.getRefundForProcessing(21L)).thenReturn(pending);
        when(tokenProvider.authorizationHeader()).thenReturn("Bearer internal");
        when(inventoryClient.applyStockOperation(any(), eq("Bearer internal")))
                .thenReturn(new StockOperationClientResponse(
                        "refund:21:restore:7", 3L, 7L, 1, "RESTORE", "REJECTED"));

        assertThrows(ResponseStatusException.class, () -> service.processRefund(21L));

        verify(coordinator).failRefund(21L);
        verify(coordinator, never()).completeRefund(21L);
    }

    @Test
    void returnRequestUsesOrderPaymentMethodAndStableRefundIdempotencyKey() throws Exception {
        Order order = Order.builder().id(30L).branchId(3L).cashierId(42L)
                .status(OrderStatus.COMPLETED).paymentType(PaymentType.CARD)
                .totalAmount(new BigDecimal("12.50")).items(List.of(item(7L, 1))).build();
        when(orderRepository.findWithItemsAndCustomerById(30L)).thenReturn(Optional.of(order));
        when(userOrgClient.canCurrentUserAccessBranch(3L, null, "ORDER_MANAGE")).thenReturn(false);
        when(userOrgClient.canCurrentUserAccessBranch(3L, null, "ORDER_CREATE")).thenReturn(true);
        when(userOrgClient.getCurrentUserProfile())
                .thenReturn(new CurrentUserDTO(42L, "cashier@example.test", "Cashier", 8L, 3L));
        when(refundRepository.findFirstByOrderIdOrderByIdAsc(30L)).thenReturn(Optional.empty());
        when(tokenProvider.authorizationHeader()).thenReturn("Bearer internal");

        Refund pending = Refund.builder().id(45L).order(order).branchId(3L).reason("Damaged product")
                .paymentType(PaymentType.CARD).processingStatus("PENDING")
                .paymentRefundStatus(PaymentRefundStatus.NOT_ISSUED).build();
        Refund completed = Refund.builder().id(45L).order(order).branchId(3L).reason("Damaged product")
                .paymentType(PaymentType.CARD).processingStatus("COMPLETED")
                .paymentRefundStatus(PaymentRefundStatus.MANUAL_REVIEW_REQUIRED).build();
        when(coordinator.startPendingRefund(eq(30L), any(Refund.class))).thenReturn(pending);
        when(coordinator.getRefundForProcessing(45L)).thenReturn(pending, completed);
        when(inventoryClient.applyStockOperation(any(), eq("Bearer internal")))
                .thenReturn(response("refund:45:restore:7", 7L, 1));

        Refund result = service.createRefund(RefundDTO.builder().orderId(30L)
                .reason("Damaged product").paymentType(PaymentType.CASH).build());

        ArgumentCaptor<Refund> candidate = ArgumentCaptor.forClass(Refund.class);
        verify(coordinator).startPendingRefund(eq(30L), candidate.capture());
        org.junit.jupiter.api.Assertions.assertEquals("order-refund:30", candidate.getValue().getIdempotencyKey());
        org.junit.jupiter.api.Assertions.assertEquals(PaymentType.CARD, candidate.getValue().getPaymentType());
        org.junit.jupiter.api.Assertions.assertEquals(PaymentRefundStatus.NOT_ISSUED,
                candidate.getValue().getPaymentRefundStatus());
        org.junit.jupiter.api.Assertions.assertEquals(PaymentRefundStatus.MANUAL_REVIEW_REQUIRED,
                result.getPaymentRefundStatus());
        verify(coordinator).completeRefund(45L);
    }

    @Test
    void duplicateCompletedRefundRequestReturnsExistingRecordWithoutRestoringStockAgain() throws Exception {
        Order order = Order.builder().id(30L).branchId(3L).cashierId(42L)
                .status(OrderStatus.REFUND_PENDING_REVIEW).paymentType(PaymentType.CASH).build();
        Refund existing = Refund.builder().id(45L).order(order).branchId(3L).cashierId(42L)
                .reason("Damaged product").processingStatus("COMPLETED")
                .paymentRefundStatus(PaymentRefundStatus.MANUAL_REVIEW_REQUIRED).build();
        when(orderRepository.findWithItemsAndCustomerById(30L)).thenReturn(Optional.of(order));
        when(userOrgClient.canCurrentUserAccessBranch(3L, null, "ORDER_MANAGE")).thenReturn(false);
        when(userOrgClient.canCurrentUserAccessBranch(3L, null, "ORDER_CREATE")).thenReturn(true);
        when(userOrgClient.getCurrentUserProfile())
                .thenReturn(new CurrentUserDTO(42L, "cashier@example.test", "Cashier", 8L, 3L));
        when(refundRepository.findFirstByOrderIdOrderByIdAsc(30L)).thenReturn(Optional.of(existing));

        Refund result = service.createRefund(RefundDTO.builder().orderId(30L)
                .reason("Damaged product").build());

        org.junit.jupiter.api.Assertions.assertSame(existing, result);
        verify(coordinator, never()).startPendingRefund(any(), any());
        verify(inventoryClient, never()).applyStockOperation(any(), any());
    }

    @Test
    void refundCannotBeCreatedByAUserOutsideTheOrderBranch() throws Exception {
        Order order = Order.builder().id(30L).branchId(3L).cashierId(42L)
                .status(OrderStatus.COMPLETED).paymentType(PaymentType.CASH).build();
        when(orderRepository.findWithItemsAndCustomerById(30L)).thenReturn(Optional.of(order));
        when(userOrgClient.canCurrentUserAccessBranch(3L, null, "ORDER_MANAGE")).thenReturn(false);
        when(userOrgClient.getCurrentUserProfile())
                .thenReturn(new CurrentUserDTO(99L, "other@example.test", "Other", 9L, 9L));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.createRefund(RefundDTO.builder().orderId(30L)
                        .reason("Damaged product").build()));
        verify(coordinator, never()).startPendingRefund(any(), any());
    }

    private Refund refundWithItems(OrderItem... items) {
        Order order = Order.builder().id(30L).branchId(3L).status(OrderStatus.REFUNDING)
                .items(List.of(items)).build();
        return Refund.builder().id(20L).order(order).branchId(3L)
                .amount(new BigDecimal("12.50")).paymentType(PaymentType.CASH)
                .processingStatus("PENDING").build();
    }

    private OrderItem item(Long productId, int quantity) {
        return OrderItem.builder().productId(productId).quantity(quantity).build();
    }

    private StockOperationClientResponse response(String key, Long productId, int quantity) {
        return new StockOperationClientResponse(key, 3L, productId, quantity, "RESTORE", "APPLIED");
    }
}
