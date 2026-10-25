package com.zosh.service;

import com.zosh.domain.OrderWorkflowState;
import com.zosh.domain.ReservationState;
import com.zosh.domain.RestorationState;
import com.zosh.modal.Order;
import com.zosh.modal.OrderWorkflow;
import com.zosh.modal.OrderWorkflowStep;
import com.zosh.payload.dto.StockOperationClientRequest;
import com.zosh.payload.dto.StockOperationClientResponse;
import com.zosh.client.InventoryClient;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderWorkflowProcessor {
    private final OrderWorkflowCoordinator coordinator;
    private final InventoryClient inventoryClient;
    private final ServiceTokenProvider serviceTokenProvider;

    public Order processOrder(Long workflowId) {
        OrderWorkflow workflow = coordinator.getWorkflow(workflowId);
        Long orderId = workflow.getOrder().getId();
        String authorization = serviceTokenProvider.authorizationHeader();

        if (workflow.getState() == OrderWorkflowState.RESERVING) {
            for (OrderWorkflowStep step : workflow.getSteps()) {
                if (step.getReservationState() == ReservationState.RESERVED) continue;
                if (step.getReservationState() == ReservationState.REJECTED) {
                    coordinator.beginCompensation(workflowId);
                    break;
                }
                StockOperationClientResponse response = apply(workflow, step,
                        step.getReservationOperationKey(), "RESERVE", authorization);
                if ("APPLIED".equals(response.status())) {
                    coordinator.markReserved(step.getId());
                } else if ("REJECTED".equals(response.status())) {
                    coordinator.markReservationRejected(step.getId());
                    coordinator.beginCompensation(workflowId);
                    break;
                } else {
                    throw new IllegalStateException("Inventory returned an unrecognized reservation result");
                }
            }
            workflow = coordinator.getWorkflow(workflowId);
            if (workflow.getState() == OrderWorkflowState.RESERVING) {
                coordinator.finishWorkflow(workflowId);
            }
        }

        workflow = coordinator.getWorkflow(workflowId);
        if (workflow.getState() == OrderWorkflowState.COMPENSATING
                || workflow.getState() == OrderWorkflowState.CANCELLING) {
            for (OrderWorkflowStep step : workflow.getSteps()) {
                if (step.getReservationState() != ReservationState.RESERVED
                        || step.getRestorationState() == RestorationState.RESTORED) continue;
                StockOperationClientResponse response = apply(workflow, step,
                        step.getRestorationOperationKey(), "RESTORE", authorization);
                if ("APPLIED".equals(response.status())) {
                    coordinator.markRestored(step.getId());
                } else if ("REJECTED".equals(response.status())) {
                    coordinator.failWorkflow(workflowId, "Inventory rejected a stock restoration; manual reconciliation is required");
                    break;
                } else {
                    throw new IllegalStateException("Inventory returned an unrecognized restoration result");
                }
            }
            coordinator.finishCompensation(workflowId);
        }
        return coordinator.getOrder(orderId);
    }

    public Order resumeOrder(Long orderId) {
        Long workflowId = coordinator.getWorkflowIdForOrder(orderId);
        if (workflowId == null) return coordinator.getOrder(orderId);
        return processOrder(workflowId);
    }

    public Order cancelOrder(Long orderId) {
        coordinator.startCancellation(orderId);
        Long workflowId = coordinator.getWorkflowIdForOrder(orderId);
        return processOrder(workflowId);
    }

    private StockOperationClientResponse apply(OrderWorkflow workflow, OrderWorkflowStep step,
                                                String operationKey, String operationType,
                                                String authorization) {
        StockOperationClientRequest request = new StockOperationClientRequest(operationKey,
                workflow.getBranchId(), step.getProductId(), step.getQuantity(), operationType);
        StockOperationClientResponse response = inventoryClient.applyStockOperation(request, authorization);
        if (response == null || !operationKey.equals(response.operationKey())
                || !workflow.getBranchId().equals(response.branchId())
                || !step.getProductId().equals(response.productId())
                || !step.getQuantity().equals(response.quantity())
                || !operationType.equals(response.operationType())) {
            throw new IllegalStateException("Inventory returned a result that does not match the requested operation");
        }
        return response;
    }
}
