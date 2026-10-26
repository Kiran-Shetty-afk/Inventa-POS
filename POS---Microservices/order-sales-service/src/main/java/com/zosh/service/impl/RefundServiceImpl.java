package com.zosh.service.impl;

import com.zosh.client.UserOrgClient;
import com.zosh.client.InventoryClient;
import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentRefundStatus;
import com.zosh.exception.ResourceNotFoundException;
import com.zosh.exception.UserException;
import com.zosh.modal.Order;
import com.zosh.modal.OrderItem;
import com.zosh.modal.Refund;
import com.zosh.modal.RefundItem;
import com.zosh.modal.ShiftReport;
import com.zosh.payload.dto.RefundDTO;
import com.zosh.payload.dto.StockOperationClientRequest;
import com.zosh.payload.dto.StockOperationClientResponse;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.RefundRepository;
import com.zosh.repository.ShiftReportRepository;
import com.zosh.service.RefundService;
import com.zosh.service.OrderWorkflowCoordinator;
import com.zosh.service.LoyaltyPointsService;
import com.zosh.service.ServiceTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final ShiftReportRepository shiftReportRepository;
    private final InventoryClient inventoryClient;
    private final ServiceTokenProvider serviceTokenProvider;
    private final UserOrgClient userOrgClient;
    private final OrderWorkflowCoordinator workflowCoordinator;
    private final LoyaltyPointsService loyaltyPointsService;

    @Override
    public Refund createRefund(RefundDTO refundDTO) throws UserException, ResourceNotFoundException {
        if (refundDTO == null || refundDTO.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID is mandatory for processing a refund");
        }
        String requestedReason = refundDTO.getReason() == null ? "" : refundDTO.getReason().trim();
        if (requestedReason.isBlank() || requestedReason.length() > 255) {
            throw new IllegalArgumentException("A refund reason between 1 and 255 characters is required");
        }
        String requestedKey = refundDTO.getIdempotencyKey() == null ? "" : refundDTO.getIdempotencyKey().trim();
        if (!requestedKey.matches("[A-Za-z0-9._:-]{8,56}")) {
            throw new IllegalArgumentException("A valid refund idempotency key is required");
        }

        Order order = orderRepository.findWithItemsAndCustomerById(refundDTO.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + refundDTO.getOrderId()));

        boolean mayManageBranch = order.getBranchId() != null && Boolean.TRUE.equals(
                userOrgClient.canCurrentUserAccessBranch(order.getBranchId(), null, "ORDER_MANAGE"));
        var currentUser = userOrgClient.getCurrentUserProfile();
        boolean isOriginalCashier = order.getCashierId() != null && currentUser != null
                && order.getCashierId().equals(currentUser.id())
                && order.getBranchId() != null
                && Boolean.TRUE.equals(userOrgClient.canCurrentUserAccessBranch(order.getBranchId(), null, "ORDER_CREATE"));
        if (!mayManageBranch && !isOriginalCashier) {
            throw new AccessDeniedException("You are not authorized to refund this order");
        }

        Refund existingRefund = refundRepository.findByIdempotencyKey("refund:" + requestedKey).orElse(null);
        if (existingRefund != null) {
            if (!Objects.equals(existingRefund.getOrder().getId(), order.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This refund request key is already in use");
            }
            if ("PENDING".equals(existingRefund.getProcessingStatus())) {
                return processRefund(existingRefund.getId());
            }
            if ("COMPLETED".equals(existingRefund.getProcessingStatus())) {
                return existingRefund;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The existing refund requires manual inventory reconciliation");
        }

        if (order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.PARTIALLY_REFUNDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is not currently eligible for a refund");
        }

        Long branchId = order.getBranchId();
        if (branchId == null) {
            throw new IllegalArgumentException("Order branch is required to process a refund");
        }
        Long cashierId = order.getCashierId();
        String cashierName = "system";

        ShiftReport activeShift = null;
        if (cashierId != null) {
            activeShift = shiftReportRepository.findTopByCashierIdAndShiftEndIsNullOrderByShiftStartDesc(cashierId)
                    .orElse(null);
        }

        Refund refund = Refund.builder()
                .order(order)
                .reason(requestedReason)
                .amount(BigDecimal.ZERO.setScale(2))
                .idempotencyKey("refund:" + requestedKey)
                .cashierId(cashierId)
                .cashierName(cashierName)
                .branchId(branchId)
                .shiftReport(activeShift)
                // The refund method is not a client decision; this API does not issue money.
                .paymentType(order.getPaymentType())
                .paymentRefundStatus(PaymentRefundStatus.NOT_ISSUED)
                .processingStatus("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        Refund savedRefund;
        try {
            savedRefund = workflowCoordinator.startPendingRefund(order.getId(), refund, refundDTO.getItems());
        } catch (DataIntegrityViolationException duplicateRequest) {
            Refund winner = refundRepository.findByIdempotencyKey("refund:" + requestedKey).orElse(null);
            if (winner == null || !Objects.equals(winner.getOrder().getId(), order.getId())) throw duplicateRequest;
            if ("PENDING".equals(winner.getProcessingStatus())) {
                return processRefund(winner.getId());
            }
            if ("COMPLETED".equals(winner.getProcessingStatus())) {
                return winner;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The existing refund requires manual inventory reconciliation");
        }
        return processRefund(savedRefund.getId());
    }

    public Refund processRefund(Long refundId) {
        Refund refund = workflowCoordinator.getRefundForProcessing(refundId);
        if ("COMPLETED".equals(refund.getProcessingStatus())) return refund;
        if (!"PENDING".equals(refund.getProcessingStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Refund requires manual inventory reconciliation");
        }

        Map<Long, Integer> quantities = new TreeMap<>();
        List<RefundItem> items = refund.getItems() == null ? List.of() : refund.getItems();
        if (items != null) {
            for (RefundItem item : items) {
                if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Refund order contains an invalid item and requires manual review");
                }
                quantities.merge(item.getProductId(), item.getQuantity(), Math::addExact);
            }
        }
        if (quantities.isEmpty()) {
            workflowCoordinator.failRefund(refundId);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Refund has no restorable order items and requires manual review");
        }

        String authorization = serviceTokenProvider.authorizationHeader();
        for (Map.Entry<Long, Integer> item : quantities.entrySet()) {
            String operationKey = "refund:" + refundId + ":restore:" + item.getKey();
            StockOperationClientRequest request = new StockOperationClientRequest(
                    operationKey, refund.getBranchId(), item.getKey(), item.getValue(), "RESTORE");
            StockOperationClientResponse response;
            try {
                response = inventoryClient.applyStockOperation(request, authorization);
            } catch (RuntimeException exception) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Refund restoration is pending and will be retried safely");
            }
            if (response == null || !operationKey.equals(response.operationKey())
                    || !refund.getBranchId().equals(response.branchId())
                    || !item.getKey().equals(response.productId())
                    || !item.getValue().equals(response.quantity())
                    || !"RESTORE".equals(response.operationType())) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Refund restoration result is uncertain and will be retried safely");
            }
            if ("REJECTED".equals(response.status())) {
                workflowCoordinator.failRefund(refundId);
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Inventory rejected refund stock restoration; manual reconciliation is required");
            }
            if (!"APPLIED".equals(response.status())) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Refund restoration result is uncertain and will be retried safely");
            }
        }
        workflowCoordinator.completeRefund(refundId);
        return workflowCoordinator.getRefundForProcessing(refundId);
    }

    public void recoverPendingRefunds() {
        for (Refund refund : refundRepository.findByProcessingStatus("PENDING")) {
            try {
                processRefund(refund.getId());
            } catch (RuntimeException exception) {
                log.warn("Refund workflow {} remains recoverable after a processing error", refund.getId());
            }
        }
    }

    @Override
    public List<Refund> getAllRefunds() {
        return refundRepository.findAll();
    }

    @Override
    public List<Refund> getRefundsByCashier(Long cashierId) {
        return refundRepository.findByCashierId(cashierId);
    }

    @Override
    public List<Refund> getRefundsByShiftReport(Long shiftReportId) {
        return refundRepository.findByShiftReportId(shiftReportId);
    }

    @Override
    public List<Refund> getRefundsByCashierAndDateRange(Long cashierId, LocalDateTime from, LocalDateTime to) {
        return refundRepository.findByCashierIdAndCreatedAtBetween(cashierId, from, to);
    }

    @Override
    public List<Refund> getRefundsByBranch(Long branchId) {
        return refundRepository.findByBranchId(branchId);
    }

    @Override
    public List<Refund> getRefundsByOrder(Long orderId) throws ResourceNotFoundException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        boolean mayManageBranch = order.getBranchId() != null && Boolean.TRUE.equals(
                userOrgClient.canCurrentUserAccessBranch(order.getBranchId(), null, "ORDER_MANAGE"));
        var currentUser = userOrgClient.getCurrentUserProfile();
        boolean isOriginalCashier = order.getCashierId() != null && currentUser != null
                && order.getCashierId().equals(currentUser.id()) && order.getBranchId() != null
                && Boolean.TRUE.equals(userOrgClient.canCurrentUserAccessBranch(order.getBranchId(), null, "ORDER_CREATE"));
        if (!mayManageBranch && !isOriginalCashier) {
            throw new AccessDeniedException("You are not authorized to view refunds for this order");
        }
        return refundRepository.findByOrderIdOrderByIdAsc(orderId);
    }

    @Override
    public Refund getRefundById(Long id) throws ResourceNotFoundException {
        return refundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found with id: " + id));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Refund confirmPaymentRefund(Long id) {
        Refund refund = refundRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found with id: " + id));
        Order order = orderRepository.findByIdForUpdate(refund.getOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (refund.getPaymentRefundStatus() == PaymentRefundStatus.SUCCEEDED) {
            return refund;
        }
        if (!"COMPLETED".equals(refund.getProcessingStatus())
                || refund.getPaymentRefundStatus() != PaymentRefundStatus.MANUAL_REVIEW_REQUIRED
                || order.getStatus() != OrderStatus.REFUND_PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only a refund awaiting payment review can be marked as refunded");
        }

        var currentUser = userOrgClient.getCurrentUserProfile();
        if (currentUser == null || currentUser.id() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The confirming user could not be verified");
        }

        refund.setPaymentRefundStatus(PaymentRefundStatus.SUCCEEDED);
        refund.setPaymentRefundConfirmedAt(LocalDateTime.now());
        refund.setPaymentRefundConfirmedBy(currentUser.id());
        Refund confirmed = refundRepository.saveAndFlush(refund);
        BigDecimal cumulativeConfirmed = refundRepository.sumAmountByOrderIdAndPaymentRefundStatus(
                order.getId(), PaymentRefundStatus.SUCCEEDED);
        loyaltyPointsService.reverseForConfirmedRefund(order, confirmed,
                cumulativeConfirmed == null ? BigDecimal.ZERO : cumulativeConfirmed);
        int purchased = order.getItems() == null ? 0 : order.getItems().stream()
                .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity()).sum();
        Integer reserved = order.getId() == null ? 0 : workflowCoordinator.getRefundedQuantity(order.getId());
        boolean legacyFullRefund = refund.getItems() == null || refund.getItems().isEmpty();
        order.setStatus(legacyFullRefund || (purchased > 0 && reserved != null && reserved >= purchased)
                ? OrderStatus.REFUNDED : OrderStatus.PARTIALLY_REFUNDED);
        orderRepository.save(order);
        return confirmed;
    }

}
