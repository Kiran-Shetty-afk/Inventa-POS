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
import com.zosh.modal.OrderItem;
import com.zosh.modal.RefundItem;
import com.zosh.payload.dto.RefundItemDTO;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.RefundRepository;
import com.zosh.repository.RefundItemRepository;
import com.zosh.service.LoyaltyPointsService;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class OrderWorkflowCoordinator {
    private final OrderRepository orderRepository;
    private final OrderWorkflowRepository workflowRepository;
    private final OrderWorkflowStepRepository stepRepository;
    private final RefundRepository refundRepository;
    private final RefundItemRepository refundItemRepository;
    private final LoyaltyPointsService loyaltyPointsService;

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
            loyaltyPointsService.creditForOrder(order);
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
        return startPendingRefund(orderId, candidate, null);
    }

    @Transactional
    public Refund startPendingRefund(Long orderId, Refund candidate, List<RefundItemDTO> requestedItems) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        Refund duplicate = candidate.getIdempotencyKey() == null ? null
                : refundRepository.findByIdempotencyKey(candidate.getIdempotencyKey()).orElse(null);
        if (duplicate != null) {
            if (!orderId.equals(duplicate.getOrder().getId())) {
                throw new IllegalStateException("The idempotency key belongs to a different order");
            }
            return duplicate;
        }
        if (order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.PARTIALLY_REFUNDED) {
            throw new IllegalStateException("This order is not currently eligible for a refund");
        }
        order = orderRepository.findWithItemsAndCustomerById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        List<Refund> priorRefunds = refundRepository.findByOrderIdOrderByIdAsc(orderId);
        if (priorRefunds.stream().anyMatch(prior -> prior.getItems() == null || prior.getItems().isEmpty())) {
            throw new IllegalStateException("An earlier refund has no item audit; manual reconciliation is required");
        }
        List<OrderItem> orderItems = order.getItems() == null ? List.of() : order.getItems().stream()
                .sorted(Comparator.comparing(OrderItem::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (orderItems.isEmpty()) throw new IllegalStateException("Order has no refundable items");
        Map<Long, Integer> requested = normalizeRequestedQuantities(orderItems, requestedItems);
        Map<Long, BigDecimal> allocations = calculateFullItemAllocations(order, orderItems);
        BigDecimal totalRefund = BigDecimal.ZERO.setScale(2);
        for (OrderItem item : orderItems) {
            Integer quantity = requested.get(item.getId());
            if (quantity == null) continue;
            int purchased = item.getQuantity() == null ? 0 : item.getQuantity();
            int alreadyReserved = reservedQuantity(item.getId());
            int remaining = purchased - alreadyReserved;
            if (quantity <= 0 || quantity > remaining) {
                throw new IllegalArgumentException("Refund quantity exceeds the remaining quantity for " + item.getProductName());
            }
            BigDecimal fullAllocation = allocations.get(item.getId());
            BigDecimal lineAmount = proportional(fullAllocation, alreadyReserved + quantity, purchased)
                    .subtract(proportional(fullAllocation, alreadyReserved, purchased))
                    .setScale(2, RoundingMode.HALF_UP);
            if (lineAmount.signum() <= 0) throw new IllegalArgumentException("Calculated refund amount must be positive");
            candidate.addItem(RefundItem.builder().orderItem(item).productId(item.getProductId())
                    .quantity(quantity).amount(lineAmount).build());
            totalRefund = totalRefund.add(lineAmount).setScale(2, RoundingMode.HALF_UP);
        }
        if (candidate.getItems().isEmpty()) throw new IllegalArgumentException("Select at least one refundable item");
        BigDecimal confirmedAmount = refundRepository.sumAmountByOrderIdAndPaymentRefundStatus(
                orderId, PaymentRefundStatus.SUCCEEDED);
        if (confirmedAmount == null) confirmedAmount = BigDecimal.ZERO;
        if (totalRefund.add(confirmedAmount).compareTo(order.getTotalAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount exceeds the order's remaining refundable balance");
        }
        candidate.setAmount(totalRefund);
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

    @Transactional(readOnly = true)
    public Integer getRefundedQuantity(Long orderId) {
        return refundItemRepository.sumReservedQuantityByOrderId(orderId);
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

    private Map<Long, Integer> normalizeRequestedQuantities(List<OrderItem> items, List<RefundItemDTO> request) {
        Map<Long, Integer> requested = new HashMap<>();
        if (request == null || request.isEmpty()) {
            for (OrderItem item : items) {
                int remaining = (item.getQuantity() == null ? 0 : item.getQuantity()) - reservedQuantity(item.getId());
                if (remaining > 0) requested.put(item.getId(), remaining);
            }
            return requested;
        }
        Set<Long> validIds = items.stream().map(OrderItem::getId).collect(Collectors.toSet());
        for (RefundItemDTO line : request) {
            if (line == null || line.getOrderItemId() == null || line.getQuantity() == null
                    || !validIds.contains(line.getOrderItemId())
                    || requested.putIfAbsent(line.getOrderItemId(), line.getQuantity()) != null) {
                throw new IllegalArgumentException("Refund items must identify unique items from this order");
            }
        }
        return requested;
    }

    private int reservedQuantity(Long orderItemId) {
        Integer quantity = refundItemRepository.sumReservedQuantityByOrderItemId(orderItemId);
        return quantity == null ? 0 : quantity;
    }

    private Map<Long, BigDecimal> calculateFullItemAllocations(Order order, List<OrderItem> items) {
        BigDecimal basisTotal = items.stream().map(this::itemBasis).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (basisTotal.signum() <= 0) throw new IllegalArgumentException("Order has no refundable value");
        BigDecimal subtotal = order.getSubtotalAmount() == null ? basisTotal : order.getSubtotalAmount();
        BigDecimal discount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        BigDecimal tax = order.getTaxAmount() == null ? BigDecimal.ZERO : order.getTaxAmount();
        BigDecimal total = order.getTotalAmount();
        if (total == null || total.signum() <= 0) throw new IllegalArgumentException("Order total is not refundable");
        boolean hasCompleteBreakdown = subtotal.signum() > 0 && subtotal.subtract(discount).add(tax)
                .setScale(2, RoundingMode.HALF_UP).compareTo(total.setScale(2, RoundingMode.HALF_UP)) == 0;
        Map<Long, BigDecimal> result = new HashMap<>();
        BigDecimal assigned = BigDecimal.ZERO.setScale(2);
        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            BigDecimal basis = itemBasis(item);
            BigDecimal allocation;
            if (i == items.size() - 1) {
                allocation = total.subtract(assigned).setScale(2, RoundingMode.HALF_UP);
            } else if (hasCompleteBreakdown) {
                BigDecimal discountShare = discount.multiply(basis).divide(subtotal, 2, RoundingMode.HALF_UP);
                BigDecimal taxShare = tax.multiply(basis).divide(subtotal, 2, RoundingMode.HALF_UP);
                allocation = basis.subtract(discountShare).add(taxShare).setScale(2, RoundingMode.HALF_UP);
            } else {
                allocation = total.multiply(basis).divide(basisTotal, 2, RoundingMode.HALF_UP);
            }
            result.put(item.getId(), allocation);
            assigned = assigned.add(allocation).setScale(2, RoundingMode.HALF_UP);
        }
        return result;
    }

    private BigDecimal itemBasis(OrderItem item) {
        if (item.getPrice() != null) return item.getPrice();
        if (item.getProductPrice() == null || item.getQuantity() == null) return BigDecimal.ZERO;
        return item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal proportional(BigDecimal total, int quantity, int purchased) {
        if (quantity <= 0 || purchased <= 0) return BigDecimal.ZERO.setScale(2);
        if (quantity >= purchased) return total.setScale(2, RoundingMode.HALF_UP);
        return total.multiply(BigDecimal.valueOf(quantity)).divide(BigDecimal.valueOf(purchased), 2, RoundingMode.HALF_UP);
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
