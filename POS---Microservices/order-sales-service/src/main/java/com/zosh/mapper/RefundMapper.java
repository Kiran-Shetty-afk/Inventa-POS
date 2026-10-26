package com.zosh.mapper;

import com.zosh.modal.Refund;
import com.zosh.payload.dto.RefundDTO;
import com.zosh.payload.dto.RefundItemDTO;

public class RefundMapper {

    public static RefundDTO toDTO(Refund refund) {
        if (refund == null) return null;

        RefundDTO dto = new RefundDTO();
        dto.setId(refund.getId());
        dto.setOrderId(refund.getOrder() != null ? refund.getOrder().getId() : null);
        dto.setReason(refund.getReason());
        dto.setAmount(refund.getAmount());
        dto.setCashierName(refund.getCashierName());
        dto.setCashierId(refund.getCashierId());
        dto.setBranchId(refund.getBranchId());
        dto.setShiftReportId(refund.getShiftReport() != null ? refund.getShiftReport().getId() : null);
        dto.setCreatedAt(refund.getCreatedAt());
        dto.setPaymentType(refund.getPaymentType());
        dto.setProcessingStatus(refund.getProcessingStatus());
        dto.setStockRestorationStatus(refund.getProcessingStatus());
        dto.setPaymentRefundStatus(refund.getPaymentRefundStatus());
        dto.setPaymentRefundConfirmedAt(refund.getPaymentRefundConfirmedAt());
        dto.setPaymentRefundConfirmedBy(refund.getPaymentRefundConfirmedBy());
        dto.setIdempotencyKey(refund.getIdempotencyKey());
        if (refund.getItems() != null) {
            dto.setItems(refund.getItems().stream().map(item -> RefundItemDTO.builder()
                    .orderItemId(item.getOrderItem().getId())
                    .productId(item.getProductId())
                    .productName(item.getOrderItem().getProductName())
                    .purchasedQuantity(item.getOrderItem().getQuantity())
                    .quantity(item.getQuantity())
                    .amount(item.getAmount())
                    .build()).toList());
        }
        return dto;
    }
}
