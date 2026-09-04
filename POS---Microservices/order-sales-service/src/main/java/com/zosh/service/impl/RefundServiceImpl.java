package com.zosh.service.impl;

import com.zosh.client.InventoryClient;
import com.zosh.domain.OrderStatus;
import com.zosh.exception.ResourceNotFoundException;
import com.zosh.exception.UserException;
import com.zosh.modal.Order;
import com.zosh.modal.OrderItem;
import com.zosh.modal.Refund;
import com.zosh.modal.ShiftReport;
import com.zosh.payload.dto.InventoryDTO;
import com.zosh.payload.dto.RefundDTO;
import com.zosh.repository.OrderItemRepository;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.RefundRepository;
import com.zosh.repository.ShiftReportRepository;
import com.zosh.service.RefundService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShiftReportRepository shiftReportRepository;
    private final InventoryClient inventoryClient;

    @Override
    @Transactional
    public Refund createRefund(RefundDTO refundDTO) throws UserException, ResourceNotFoundException {
        if (refundDTO.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID is mandatory for processing a refund");
        }

        Order order = orderRepository.findById(refundDTO.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + refundDTO.getOrderId()));

        if (order.getStatus() == OrderStatus.REFUNDED || refundRepository.existsByOrderId(order.getId())) {
            throw new UserException("Order is already refunded");
        }

        Long branchId = refundDTO.getBranchId() != null ? refundDTO.getBranchId() : order.getBranchId();
        Long cashierId = refundDTO.getCashierId() != null ? refundDTO.getCashierId() : order.getCashierId();
        String cashierName = refundDTO.getCashierName();

        // ============================================================
        // RESTORE INVENTORY FOR ALL ITEMS IN THE REFUNDED ORDER
        // ============================================================
        List<OrderItem> items = (order.getItems() != null && !order.getItems().isEmpty())
                ? order.getItems()
                : orderItemRepository.findByOrderId(order.getId());

        if (items != null && !items.isEmpty()) {
            for (OrderItem item : items) {
                Long productId = item.getProductId();
                int refundQty = item.getQuantity() != null ? item.getQuantity() : 0;

                if (productId == null || refundQty <= 0) {
                    continue;
                }

                log.info("Restoring inventory for refunded order ID: {}, Product ID: {}, Quantity: {}",
                        order.getId(), productId, refundQty);

                InventoryDTO inventory = inventoryClient.getInventoryByBranchAndProduct(branchId, productId);
                if (inventory == null) {
                    log.error(
                            "Failed to restore inventory: Inventory record not found for branch ID {} and product ID {}",
                            branchId, productId);
                    throw new EntityNotFoundException("Cannot restore inventory: Inventory not found for branch "
                            + branchId + " and product " + productId);
                }

                int oldQuantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
                int newQuantity = oldQuantity + refundQty;
                inventory.setQuantity(newQuantity);

                inventoryClient.updateInventory(inventory.getId(), inventory);

                log.info("Inventory restored successfully: Product ID: {}, Old Quantity: {}, New Quantity: {}",
                        productId, oldQuantity, newQuantity);
            }
        }

        ShiftReport activeShift = null;
        if (refundDTO.getShiftReportId() != null) {
            activeShift = shiftReportRepository.findById(refundDTO.getShiftReportId()).orElse(null);
        } else if (cashierId != null) {
            activeShift = shiftReportRepository.findTopByCashierIdAndShiftEndIsNullOrderByShiftStartDesc(cashierId)
                    .orElse(null);
        }

        Refund refund = Refund.builder()
                .order(order)
                .reason(refundDTO.getReason())
                .amount(order.getTotalAmount() != null ? order.getTotalAmount()
                        : (refundDTO.getAmount() != null ? refundDTO.getAmount() : 0.0))
                .cashierId(cashierId)
                .cashierName(cashierName)
                .branchId(branchId)
                .shiftReport(activeShift)
                .paymentType(refundDTO.getPaymentType() != null ? refundDTO.getPaymentType() : order.getPaymentType())
                .createdAt(LocalDateTime.now())
                .build();

        Refund savedRefund = refundRepository.save(refund);

        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);

        return savedRefund;
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
    public Refund getRefundById(Long id) throws ResourceNotFoundException {
        return refundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found with id: " + id));
    }

    @Override
    public void deleteRefund(Long refundId) throws ResourceNotFoundException {
        if (!refundRepository.existsById(refundId)) {
            throw new ResourceNotFoundException("Refund not found with id: " + refundId);
        }
        refundRepository.deleteById(refundId);
    }
}
