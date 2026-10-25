package com.zosh.mapper;

import com.zosh.modal.Order;
import com.zosh.modal.PaymentSummary;
import com.zosh.modal.Refund;
import com.zosh.modal.ShiftReport;
import com.zosh.payload.dto.OrderDTO;
import com.zosh.payload.dto.RefundDTO;
import com.zosh.payload.dto.ShiftReportDTO;
import com.zosh.payload.dto.UserDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ShiftReportMapper {

    public static ShiftReportDTO toDTO(ShiftReport shiftReport) {
        if (shiftReport == null) return null;

        ShiftReportDTO dto = new ShiftReportDTO();
        dto.setId(shiftReport.getId());
        dto.setShiftStart(shiftReport.getShiftStart());
        dto.setShiftEnd(shiftReport.getShiftEnd());
        dto.setTotalSales(shiftReport.getTotalSales() != null ? shiftReport.getTotalSales() : BigDecimal.ZERO);
        dto.setTotalRefunds(shiftReport.getTotalRefunds() != null ? shiftReport.getTotalRefunds() : BigDecimal.ZERO);
        dto.setNetSales(shiftReport.getNetSales() != null ? shiftReport.getNetSales() : BigDecimal.ZERO);
        dto.setTotalOrders(shiftReport.getTotalOrders());
        
        dto.setCashierId(shiftReport.getCashierId());
        dto.setCashierName(shiftReport.getCashierName());
        dto.setBranchId(shiftReport.getBranchId());
        dto.setBranchName(shiftReport.getBranchName());

        if (shiftReport.getCashierId() != null) {
            dto.setCashier(UserDTO.builder()
                    .id(shiftReport.getCashierId())
                    .fullName(shiftReport.getCashierName())
                    .build());
        }

        dto.setRefunds(mapRefunds(shiftReport.getRefunds()));
        dto.setTopSellingProducts(shiftReport.getTopSellingProducts() != null
                ? shiftReport.getTopSellingProducts()
                : new ArrayList<>());

        // Payment summaries mapping
        List<PaymentSummary> summaries = shiftReport.getPaymentSummaries();
        if (summaries == null || summaries.isEmpty()) {
            summaries = buildPaymentSummariesFromShift(shiftReport);
        }
        dto.setPaymentSummaries(summaries != null ? summaries : new ArrayList<>());

        if (shiftReport.getRecentOrders() != null && !shiftReport.getRecentOrders().isEmpty()) {
            dto.setRecentOrders(mapOrders(shiftReport.getRecentOrders()));
        }

        return dto;
    }

    private static List<PaymentSummary> buildPaymentSummariesFromShift(ShiftReport shift) {
        List<PaymentSummary> list = new ArrayList<>();
        BigDecimal totalSales = shift.getTotalSales() != null ? shift.getTotalSales() : BigDecimal.ZERO;

        if (shift.getCashSales() != null && shift.getCashSales().signum() > 0) {
            BigDecimal amount = shift.getCashSales();
            double pct = totalSales.signum() > 0 ? amount.multiply(BigDecimal.valueOf(100))
                    .divide(totalSales, 4, RoundingMode.HALF_UP).doubleValue() : 0.0;
            list.add(new PaymentSummary(com.zosh.domain.PaymentType.CASH, amount, 1, pct));
        }
        if (shift.getCardSales() != null && shift.getCardSales().signum() > 0) {
            BigDecimal amount = shift.getCardSales();
            double pct = totalSales.signum() > 0 ? amount.multiply(BigDecimal.valueOf(100))
                    .divide(totalSales, 4, RoundingMode.HALF_UP).doubleValue() : 0.0;
            list.add(new PaymentSummary(com.zosh.domain.PaymentType.CARD, amount, 1, pct));
        }
        if (shift.getUpiSales() != null && shift.getUpiSales().signum() > 0) {
            BigDecimal amount = shift.getUpiSales();
            double pct = totalSales.signum() > 0 ? amount.multiply(BigDecimal.valueOf(100))
                    .divide(totalSales, 4, RoundingMode.HALF_UP).doubleValue() : 0.0;
            list.add(new PaymentSummary(com.zosh.domain.PaymentType.UPI, amount, 1, pct));
        }
        return list;
    }

    public static ShiftReportDTO toDTOWithDetails(ShiftReport shiftReport, List<Order> recentOrders, List<PaymentSummary> paymentSummaries) {
        ShiftReportDTO dto = toDTO(shiftReport);
        if (dto == null) return null;

        if (recentOrders != null) {
            dto.setRecentOrders(mapOrders(recentOrders));
        }
        if (paymentSummaries != null) {
            dto.setPaymentSummaries(paymentSummaries);
        }
        return dto;
    }

    private static List<OrderDTO> mapOrders(List<Order> orders) {
        if (orders == null) return List.of();
        return orders.stream()
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }

    private static List<RefundDTO> mapRefunds(List<Refund> refunds) {
        if (refunds == null) return List.of();
        return refunds.stream()
                .map(RefundMapper::toDTO)
                .collect(Collectors.toList());
    }
}
