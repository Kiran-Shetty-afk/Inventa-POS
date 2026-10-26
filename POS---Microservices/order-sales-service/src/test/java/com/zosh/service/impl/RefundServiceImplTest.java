package com.zosh.service.impl;

import com.zosh.client.InventoryClient;
import com.zosh.client.UserOrgClient;
import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentRefundStatus;
import com.zosh.domain.PaymentType;
import com.zosh.modal.Order;
import com.zosh.modal.OrderItem;
import com.zosh.modal.Refund;
import com.zosh.modal.RefundItem;
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
        String key = "stable-key-123";
        when(refundRepository.findByIdempotencyKey("refund:" + key)).thenReturn(Optional.empty());
        when(tokenProvider.authorizationHeader()).thenReturn("Bearer internal");

        Refund pending = refundWithItems(item(7L, 1));
        pending.setId(45L);
        pending.setOrder(order);
        pending.setReason("Damaged product");
        pending.setPaymentType(PaymentType.CARD);
        Refund completed = refundWithItems(item(7L, 1));
        completed.setId(45L);
        completed.setOrder(order);
        completed.setReason("Damaged product");
        completed.setPaymentType(PaymentType.CARD);
        completed.setProcessingStatus("COMPLETED");
        completed.setPaymentRefundStatus(PaymentRefundStatus.MANUAL_REVIEW_REQUIRED);
        when(coordinator.startPendingRefund(eq(30L), any(Refund.class), any())).thenReturn(pending);
        when(coordinator.getRefundForProcessing(45L)).thenReturn(pending, completed);
        when(inventoryClient.applyStockOperation(any(), eq("Bearer internal")))
                .thenReturn(response("refund:45:restore:7", 7L, 1));

        Refund result = service.createRefund(RefundDTO.builder().orderId(30L)
                .reason("Damaged product").idempotencyKey(key)
                .items(List.of(com.zosh.payload.dto.RefundItemDTO.builder()
                        .orderItemId(70L).quantity(1).build())).paymentType(PaymentType.CASH).build());

        ArgumentCaptor<Refund> candidate = ArgumentCaptor.forClass(Refund.class);
        verify(coordinator).startPendingRefund(eq(30L), candidate.capture(), any());
        org.junit.jupiter.api.Assertions.assertEquals("refund:" + key, candidate.getValue().getIdempotencyKey());
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
        String key = "duplicate-key-123";
        existing.setIdempotencyKey("refund:" + key);
        when(refundRepository.findByIdempotencyKey("refund:" + key)).thenReturn(Optional.of(existing));

        Refund result = service.createRefund(RefundDTO.builder().orderId(30L)
                .reason("Damaged product").idempotencyKey(key).build());

        org.junit.jupiter.api.Assertions.assertSame(existing, result);
        verify(coordinator, never()).startPendingRefund(any(), any(), any());
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
                        .reason("Damaged product").idempotencyKey("valid-key-123").build()));
        verify(coordinator, never()).startPendingRefund(any(), any(), any());
    }

    private Refund refundWithItems(OrderItem... items) {
        Order order = Order.builder().id(30L).branchId(3L).status(OrderStatus.REFUNDING)
                .items(List.of(items)).build();
        java.util.List<RefundItem> refundItems = java.util.Arrays.stream(items).map(item ->
                RefundItem.builder().productId(item.getProductId()).quantity(item.getQuantity())
                        .amount(new BigDecimal("12.50")).orderItem(item).build()).toList();
        return Refund.builder().id(20L).order(order).branchId(3L).items(refundItems)
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
