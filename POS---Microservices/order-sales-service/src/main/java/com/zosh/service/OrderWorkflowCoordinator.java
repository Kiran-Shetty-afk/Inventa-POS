package com.zosh.service;

import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentRefundStatus;
import com.zosh.domain.OrderWorkflowState;
import com.zosh.domain.ReservationState;
import com.zosh.domain.RestorationState;
import com.zosh.modal.Customer;
import com.zosh.modal.Order;
import com.zosh.modal.OrderWorkflow;
import com.zosh.modal.OrderWorkflowStep;
import com.zosh.modal.Refund;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.RefundRepository;
import com.zosh.repository.OrderWorkflowRepository;
import com.zosh.repository.OrderWorkflowStepRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderWorkflowCoordinator {
    private final OrderRepository orderRepository;
    private final OrderWorkflowRepository workflowRepository;
    private final OrderWorkflowStepRepository stepRepository;
    private final RefundRepository refundRepository;

    @Transactional
    public Long startPending(Order order, Map<Long, Integer> requestedStock) {
        order.setStatus(OrderStatus.PENDING);
        Order savedOrder = orderRepository.saveAndFlush(order);

        OrderWorkflow workflow = new OrderWorkflow();
        workflow.setOrder(savedOrder);
        workflow.setBranchId(savedOrder.getBranchId());
        workflow.setState(OrderWorkflowState.RESERVING);
        for (Map.Entry<Long, Integer> item : requestedStock.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            OrderWorkflowStep step = new OrderWorkflowStep();
            step.setProductId(item.getKey());
            step.setQuantity(item.getValue());
            step.setReservationOperationKey("order:" + savedOrder.getIdempotencyKey() + ":reserve:" + item.getKey());
            step.setRestorationOperationKey("order:" + savedOrder.getIdempotencyKey() + ":restore:" + item.getKey());
            workflow.addStep(step);
        }
        return workflowRepository.saveAndFlush(workflow).getId();
    }

    @Transactional(readOnly = true)
    public OrderWorkflow getWorkflow(Long workflowId) {
        return workflowRepository.findById(workflowId)
                .orElseThrow(() -> new EntityNotFoundException("Order workflow not found"));
    }

    @Transactional(readOnly = true)
    public Long getWorkflowIdForOrder(Long orderId) {
        return workflowRepository.findByOrderId(orderId).map(OrderWorkflow::getId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Long> getRecoverableWorkflowIds() {
        return workflowRepository.findByStateIn(List.of(OrderWorkflowState.RESERVING,
                        OrderWorkflowState.COMPENSATING, OrderWorkflowState.CANCELLING))
                .stream().map(OrderWorkflow::getId).toList();
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return orderRepository.findWithItemsAndCustomerById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }

    @Transactional
    public void markReserved(Long stepId) {
        OrderWorkflowStep step = getStep(stepId);
        if (step.getReservationState() != ReservationState.RESERVED) {
            step.setReservationState(ReservationState.RESERVED);
            stepRepository.save(step);
        }
    }

    @Transactional
    public void markReservationRejected(Long stepId) {
        OrderWorkflowStep step = getStep(stepId);
        step.setReservationState(ReservationState.REJECTED);
        stepRepository.save(step);
    }

    @Transactional
    public void beginCompensation(Long workflowId) {
        OrderWorkflow workflow = getWorkflowEntity(workflowId);
        if (workflow.getState() == OrderWorkflowState.RESERVING) {
            workflow.setState(OrderWorkflowState.COMPENSATING);
            workflow.setFailureReason("Inventory rejected a reservation");
            workflowRepository.save(workflow);
        }
    }

    @Transactional
    public void markRestored(Long stepId) {
        OrderWorkflowStep step = getStep(stepId);
        if (step.getRestorationState() != RestorationState.RESTORED) {
            step.setRestorationState(RestorationState.RESTORED);
            stepRepository.save(step);
        }
    }

    @Transactional
    public void startCancellation(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (order.getStatus() == OrderStatus.CANCELLED) return;
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Only a completed order can be cancelled");
        }
        OrderWorkflow workflow = workflowRepository.findByOrderId(orderId).orElse(null);
        if (workflow == null) {
            workflow = new OrderWorkflow();
            workflow.setOrder(order);
            workflow.setBranchId(order.getBranchId());
            workflow.setState(OrderWorkflowState.CANCELLING);
            Map<Long, Integer> priorItems = new TreeMap<>();
            if (order.getItems() != null) {
                order.getItems().stream().filter(item -> item.getProductId() != null
                                && item.getQuantity() != null && item.getQuantity() > 0)
                        .forEach(item -> priorItems.merge(item.getProductId(), item.getQuantity(), Math::addExact));
            }
            for (Map.Entry<Long, Integer> item : priorItems.entrySet()) {
                OrderWorkflowStep step = new OrderWorkflowStep();
                step.setProductId(item.getKey());
                step.setQuantity(item.getValue());
                step.setReservationOperationKey("legacy-order:" + orderId + ":reserved:" + item.getKey());
                step.setRestorationOperationKey("cancel:legacy-order:" + orderId + ":restore:" + item.getKey());
                step.setReservationState(ReservationState.RESERVED);
                workflow.addStep(step);
            }
            workflowRepository.saveAndFlush(workflow);
        } else if (workflow.getState() != OrderWorkflowState.COMPLETED) {
            throw new IllegalStateException("Only a completed order can be cancelled");
        }
        workflow.setState(OrderWorkflowState.CANCELLING);
        order.setStatus(OrderStatus.CANCELLING);
        workflowRepository.save(workflow);
        orderRepository.save(order);
    }

    @Transactional
    public void finishWorkflow(Long workflowId) {
        OrderWorkflow workflow = getWorkflowEntity(workflowId);
        if (workflow.getSteps().stream().anyMatch(step -> step.getReservationState() != ReservationState.RESERVED)) {
            throw new IllegalStateException("An order cannot be completed before every stock reservation succeeds");
        }
        Order order = orderRepository.findById(workflow.getOrder().getId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (workflow.getState() != OrderWorkflowState.COMPLETED) {
            Customer customer = order.getCustomer();
            if (customer != null) {
                int current = customer.getLoyaltyPoints() == null ? 0 : customer.getLoyaltyPoints();
                int earned = order.getTotalAmount() == null || order.getTotalAmount().signum() <= 0
                        ? 0 : order.getTotalAmount().divideToIntegralValue(BigDecimal.valueOf(100))
                        .min(BigDecimal.valueOf(Integer.MAX_VALUE)).intValue();
                customer.setLoyaltyPoints((int) Math.min(Integer.MAX_VALUE, (long) current + earned));
            }
            order.setStatus(OrderStatus.COMPLETED);
            workflow.setState(OrderWorkflowState.COMPLETED);
            orderRepository.save(order);
            workflowRepository.save(workflow);
        }
    }

    @Transactional
    public void finishCompensation(Long workflowId) {
        OrderWorkflow workflow = getWorkflowEntity(workflowId);
        boolean allReservedRestored = workflow.getSteps().stream()
                .filter(step -> step.getReservationState() == ReservationState.RESERVED)
                .allMatch(step -> step.getRestorationState() == RestorationState.RESTORED);
        if (!allReservedRestored) return;

        Order order = orderRepository.findById(workflow.getOrder().getId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (workflow.getState() == OrderWorkflowState.CANCELLING) {
            order.setStatus(OrderStatus.CANCELLED);
            workflow.setState(OrderWorkflowState.CANCELLED);
        } else if (workflow.getState() == OrderWorkflowState.COMPENSATING) {
            order.setStatus(OrderStatus.FAILED);
            workflow.setState(OrderWorkflowState.FAILED);
        }
        orderRepository.save(order);
        workflowRepository.save(workflow);
    }

    @Transactional
    public void failWorkflow(Long workflowId, String safeReason) {
        OrderWorkflow workflow = getWorkflowEntity(workflowId);
        Order order = orderRepository.findById(workflow.getOrder().getId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        workflow.setState(OrderWorkflowState.FAILED);
        workflow.setFailureReason(safeReason);
        order.setStatus(OrderStatus.FAILED);
        workflowRepository.save(workflow);
        orderRepository.save(order);
    }

    @Transactional
    public Refund startPendingRefund(Long orderId, Refund candidate) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        Refund existing = refundRepository.findFirstByOrderIdOrderByIdAsc(orderId).orElse(null);
        if (existing != null) {
            if (candidate.getIdempotencyKey() != null
                    && candidate.getIdempotencyKey().equals(existing.getIdempotencyKey())) return existing;
            throw new IllegalStateException("This order already has a refund record");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Only a completed order can be refunded");
        }
        order.setStatus(OrderStatus.REFUNDING);
        candidate.setOrder(order);
        candidate.setProcessingStatus("PENDING");
        orderRepository.save(order);
        return refundRepository.saveAndFlush(candidate);
    }

    @Transactional(readOnly = true)
    public Refund getRefundForProcessing(Long refundId) {
        return refundRepository.findWithOrderItemsById(refundId)
                .orElseThrow(() -> new EntityNotFoundException("Refund not found"));
    }

    @Transactional
    public void completeRefund(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new EntityNotFoundException("Refund not found"));
        if (!"PENDING".equals(refund.getProcessingStatus())) return;
        Order order = orderRepository.findById(refund.getOrder().getId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        refund.setProcessingStatus("COMPLETED");
        refund.setPaymentRefundStatus(PaymentRefundStatus.MANUAL_REVIEW_REQUIRED);
        order.setStatus(OrderStatus.REFUND_PENDING_REVIEW);
        refundRepository.save(refund);
        orderRepository.save(order);
    }

    @Transactional
    public void failRefund(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new EntityNotFoundException("Refund not found"));
        if (!"PENDING".equals(refund.getProcessingStatus())) return;
        Order order = orderRepository.findById(refund.getOrder().getId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        refund.setProcessingStatus("FAILED");
        order.setStatus(OrderStatus.REFUND_FAILED);
        refundRepository.save(refund);
        orderRepository.save(order);
    }

    private OrderWorkflowStep getStep(Long stepId) {
        return stepRepository.findById(stepId)
                .orElseThrow(() -> new EntityNotFoundException("Order workflow step not found"));
    }

    private OrderWorkflow getWorkflowEntity(Long workflowId) {
        return workflowRepository.findById(workflowId)
                .orElseThrow(() -> new EntityNotFoundException("Order workflow not found"));
    }
}
