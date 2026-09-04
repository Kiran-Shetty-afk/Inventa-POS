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
        dto.setTotalSales(shiftReport.getTotalSales() != null ? shiftReport.getTotalSales() : 0.0);
        dto.setTotalRefunds(shiftReport.getTotalRefunds() != null ? shiftReport.getTotalRefunds() : 0.0);
        dto.setNetSales(shiftReport.getNetSales() != null ? shiftReport.getNetSales() : 0.0);
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
        double totalSales = shift.getTotalSales() != null ? shift.getTotalSales() : 0.0;

        if (shift.getCashSales() != null && shift.getCashSales() > 0) {
            double amount = shift.getCashSales();
            double pct = totalSales > 0 ? (amount / totalSales) * 100 : 0.0;
            list.add(new PaymentSummary(com.zosh.domain.PaymentType.CASH, amount, 1, pct));
        }
        if (shift.getCardSales() != null && shift.getCardSales() > 0) {
            double amount = shift.getCardSales();
            double pct = totalSales > 0 ? (amount / totalSales) * 100 : 0.0;
            list.add(new PaymentSummary(com.zosh.domain.PaymentType.CARD, amount, 1, pct));
        }
        if (shift.getUpiSales() != null && shift.getUpiSales() > 0) {
            double amount = shift.getUpiSales();
            double pct = totalSales > 0 ? (amount / totalSales) * 100 : 0.0;
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
