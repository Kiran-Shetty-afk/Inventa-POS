package com.zosh.mapper;

import com.zosh.modal.Refund;
import com.zosh.payload.dto.RefundDTO;

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
        return dto;
    }
}
