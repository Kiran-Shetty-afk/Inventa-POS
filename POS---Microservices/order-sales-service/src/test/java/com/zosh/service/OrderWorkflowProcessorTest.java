package com.zosh.service;

import com.zosh.client.InventoryClient;
import com.zosh.domain.OrderStatus;
import com.zosh.domain.OrderWorkflowState;
import com.zosh.domain.ReservationState;
import com.zosh.domain.RestorationState;
import com.zosh.modal.Order;
import com.zosh.modal.OrderWorkflow;
import com.zosh.modal.OrderWorkflowStep;
import com.zosh.payload.dto.StockOperationClientRequest;
import com.zosh.payload.dto.StockOperationClientResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderWorkflowProcessorTest {
    @Mock private OrderWorkflowCoordinator coordinator;
    @Mock private InventoryClient inventoryClient;
    @Mock private ServiceTokenProvider tokenProvider;
    @InjectMocks private OrderWorkflowProcessor processor;

    @Test
    void timeoutRetryReusesTheSameReservationOperationKey() {
        OrderWorkflow initial = workflow(10L, OrderWorkflowState.RESERVING,
                step(21L, 7L, 2, "order:key:reserve:7", "order:key:restore:7", ReservationState.PENDING,
                        RestorationState.PENDING));
        OrderWorkflow applied = workflow(10L, OrderWorkflowState.RESERVING,
                step(21L, 7L, 2, "order:key:reserve:7", "order:key:restore:7", ReservationState.RESERVED,
                        RestorationState.PENDING));
        Order completed = Order.builder().id(110L).status(OrderStatus.COMPLETED).build();
        when(tokenProvider.authorizationHeader()).thenReturn("Bearer internal");
        when(coordinator.getWorkflow(10L)).thenReturn(initial, initial, applied);
        when(inventoryClient.applyStockOperation(any(), eq("Bearer internal")))
                .thenThrow(new IllegalStateException("uncertain network result"))
                .thenReturn(response("order:key:reserve:7", 7L, 2, "RESERVE", "APPLIED"));
        when(coordinator.getOrder(110L)).thenReturn(completed);

        assertThrows(IllegalStateException.class, () -> processor.processOrder(10L));
        assertEquals(OrderStatus.COMPLETED, processor.processOrder(10L).getStatus());

        ArgumentCaptor<StockOperationClientRequest> request =
                ArgumentCaptor.forClass(StockOperationClientRequest.class);
        verify(inventoryClient, org.mockito.Mockito.times(2))
                .applyStockOperation(request.capture(), eq("Bearer internal"));
        assertEquals(request.getAllValues().get(0).operationKey(), request.getAllValues().get(1).operationKey());
        verify(coordinator, never()).beginCompensation(10L);
        verify(coordinator).finishWorkflow(10L);
    }

    @Test
    void partialReservationFailureRestoresOnlyConfirmedReservations() {
        OrderWorkflow reserving = workflow(11L, OrderWorkflowState.RESERVING,
                step(31L, 7L, 2, "order:key:reserve:7", "order:key:restore:7", ReservationState.PENDING,
                        RestorationState.PENDING),
                step(32L, 9L, 1, "order:key:reserve:9", "order:key:restore:9", ReservationState.PENDING,
                        RestorationState.PENDING));
        OrderWorkflow compensating = workflow(11L, OrderWorkflowState.COMPENSATING,
                step(31L, 7L, 2, "order:key:reserve:7", "order:key:restore:7", ReservationState.RESERVED,
                        RestorationState.PENDING),
                step(32L, 9L, 1, "order:key:reserve:9", "order:key:restore:9", ReservationState.REJECTED,
                        RestorationState.PENDING));
        Order failed = Order.builder().id(111L).status(OrderStatus.FAILED).build();
        when(tokenProvider.authorizationHeader()).thenReturn("Bearer internal");
        when(coordinator.getWorkflow(11L)).thenReturn(reserving, compensating);
        when(inventoryClient.applyStockOperation(any(), eq("Bearer internal")))
                .thenReturn(response("order:key:reserve:7", 7L, 2, "RESERVE", "APPLIED"))
                .thenReturn(response("order:key:reserve:9", 9L, 1, "RESERVE", "REJECTED"))
                .thenReturn(response("order:key:restore:7", 7L, 2, "RESTORE", "APPLIED"));
        when(coordinator.getOrder(111L)).thenReturn(failed);

        assertEquals(OrderStatus.FAILED, processor.processOrder(11L).getStatus());

        verify(coordinator).markReserved(31L);
        verify(coordinator).markReservationRejected(32L);
        verify(coordinator).beginCompensation(11L);
        verify(coordinator).markRestored(31L);
        verify(coordinator).finishCompensation(11L);
        ArgumentCaptor<StockOperationClientRequest> requests =
                ArgumentCaptor.forClass(StockOperationClientRequest.class);
        verify(inventoryClient, org.mockito.Mockito.times(3))
                .applyStockOperation(requests.capture(), eq("Bearer internal"));
        assertEquals("order:key:restore:7", requests.getAllValues().get(2).operationKey());
    }

    @Test
    void cancellationRestoresStockUsingItsPersistedKey() {
        OrderWorkflow cancelling = workflow(12L, OrderWorkflowState.CANCELLING,
                step(41L, 7L, 3, "order:key:reserve:7", "order:key:restore:7", ReservationState.RESERVED,
                        RestorationState.PENDING));
        Order cancelled = Order.builder().id(112L).status(OrderStatus.CANCELLED).build();
        when(tokenProvider.authorizationHeader()).thenReturn("Bearer internal");
        when(coordinator.getWorkflow(12L)).thenReturn(cancelling);
        when(coordinator.getOrder(112L)).thenReturn(cancelled);
        when(inventoryClient.applyStockOperation(any(), eq("Bearer internal")))
                .thenReturn(response("order:key:restore:7", 7L, 3, "RESTORE", "APPLIED"));

        assertEquals(OrderStatus.CANCELLED, processor.processOrder(12L).getStatus());
        verify(coordinator).markRestored(41L);
        verify(coordinator).finishCompensation(12L);
    }

    private OrderWorkflow workflow(Long id, OrderWorkflowState state, OrderWorkflowStep... steps) {
        OrderWorkflow workflow = new OrderWorkflow();
        workflow.setId(id);
        workflow.setOrder(Order.builder().id(id + 100).build());
        workflow.setBranchId(3L);
        workflow.setState(state);
        for (OrderWorkflowStep step : steps) workflow.addStep(step);
        return workflow;
    }

    private OrderWorkflowStep step(Long id, Long productId, int quantity, String reserveKey, String restoreKey,
                                   ReservationState reservationState, RestorationState restorationState) {
        OrderWorkflowStep step = new OrderWorkflowStep();
        step.setId(id);
        step.setProductId(productId);
        step.setQuantity(quantity);
        step.setReservationOperationKey(reserveKey);
        step.setRestorationOperationKey(restoreKey);
        step.setReservationState(reservationState);
        step.setRestorationState(restorationState);
        return step;
    }

    private StockOperationClientResponse response(String key, Long productId, int quantity,
                                                   String type, String status) {
        return new StockOperationClientResponse(key, 3L, productId, quantity, type, status);
    }
}
