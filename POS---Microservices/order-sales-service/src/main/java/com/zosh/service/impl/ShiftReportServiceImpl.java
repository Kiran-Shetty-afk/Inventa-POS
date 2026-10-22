package com.zosh.service.impl;

import com.zosh.domain.PaymentType;
import com.zosh.exception.UserException;
import com.zosh.modal.*;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.RefundRepository;
import com.zosh.repository.ShiftReportRepository;
import com.zosh.service.ShiftReportService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftReportServiceImpl implements ShiftReportService {

    private final ShiftReportRepository shiftReportRepository;
    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;

    @Override
    @Transactional
    public ShiftReport startShift(Long cashierId,
                                  String cashierName,
                                  Long branchId,
                                  String branchName,
                                  LocalDateTime shiftStart) throws UserException {
        if (shiftStart == null) {
            shiftStart = LocalDateTime.now();
        }

        LocalDateTime startOfDay = shiftStart.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = shiftStart.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        if (cashierId != null) {
            Optional<ShiftReport> existing = shiftReportRepository
                    .findByCashierIdAndShiftStartBetween(cashierId, startOfDay, endOfDay);

            if (existing.isPresent()) {
                return existing.get(); // Return existing shift safely for idempotent cashier login
            }
        }

        ShiftReport shift = ShiftReport.builder()
                .cashierId(cashierId)
                .cashierName(cashierName)
                .branchId(branchId)
                .branchName(branchName)
                .shiftStart(shiftStart)
                .totalSales(0.0)
                .totalRefunds(0.0)
                .netSales(0.0)
                .totalOrders(0)
                .cashSales(0.0)
                .cardSales(0.0)
                .upiSales(0.0)
                .build();

        return shiftReportRepository.save(shift);
    }

    @Override
    @Transactional
    public ShiftReport endShift(Long cashierId, LocalDateTime shiftEnd) throws UserException {
        if (shiftEnd == null) {
            shiftEnd = LocalDateTime.now();
        }

        ShiftReport shift = shiftReportRepository
                .findTopByCashierIdAndShiftEndIsNullOrderByShiftStartDesc(cashierId)
                .orElseThrow(() -> new EntityNotFoundException("Active shift report not found for cashier ID: " + cashierId));

        shift.setShiftEnd(shiftEnd);

        List<Order> orders = orderRepository.findByCashierIdAndCreatedAtBetween(
                cashierId, shift.getShiftStart(), shiftEnd
        );

        List<Refund> refunds = refundRepository.findByCashierIdAndCreatedAtBetween(
                cashierId, shift.getShiftStart(), shiftEnd
        );

        double totalRefunds = refunds.stream()
                .mapToDouble(r -> r.getAmount() != null ? r.getAmount() : 0.0)
                .sum();

        double totalSales = orders.stream()
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0)
                .sum();

        int totalOrders = orders.size();
        double netSales = totalSales - totalRefunds;

        double cashSales = orders.stream()
                .filter(o -> o.getPaymentType() == PaymentType.CASH)
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0)
                .sum();

        double cardSales = orders.stream()
                .filter(o -> o.getPaymentType() == PaymentType.CARD)
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0)
                .sum();

        double upiSales = orders.stream()
                .filter(o -> o.getPaymentType() == PaymentType.UPI)
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0)
                .sum();

        shift.setTotalSales(totalSales);
        shift.setTotalOrders(totalOrders);
        shift.setTotalRefunds(totalRefunds);
        shift.setNetSales(netSales);
        shift.setCashSales(cashSales);
        shift.setCardSales(cardSales);
        shift.setUpiSales(upiSales);
        shift.setRefunds(refunds);
        shift.setPaymentSummaries(getPaymentSummaries(orders, totalSales));
        shift.setRecentOrders(getRecentOrders(orders));

        return shiftReportRepository.save(shift);
    }

    @Override
    public ShiftReport getShiftReportById(Long id) {
        ShiftReport shift = shiftReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shift report not found with ID: " + id));

        LocalDateTime end = shift.getShiftEnd() != null ? shift.getShiftEnd() : LocalDateTime.now();
        if (shift.getCashierId() != null && shift.getShiftStart() != null) {
            List<Order> orders = orderRepository.findByCashierIdAndCreatedAtBetween(
                    shift.getCashierId(), shift.getShiftStart(), end
            );
            shift.setPaymentSummaries(getPaymentSummaries(orders, shift.getTotalSales() != null ? shift.getTotalSales() : 0.0));
            shift.setRecentOrders(getRecentOrders(orders));
        }
        return shift;
    }

    @Override
    public List<ShiftReport> getAllShiftReports() {
        return shiftReportRepository.findAll();
    }

    @Override
    public List<ShiftReport> getShiftReportsByCashier(Long cashierId) {
        return shiftReportRepository.findByCashierId(cashierId);
    }

    @Override
    public List<ShiftReport> getShiftReportsByBranch(Long branchId) {
        return shiftReportRepository.findByBranchId(branchId);
    }

    @Override
    public ShiftReport getCurrentShiftProgress(Long cashierId) throws UserException {
        ShiftReport shift = shiftReportRepository
                .findTopByCashierIdAndShiftEndIsNullOrderByShiftStartDesc(cashierId)
                .orElseThrow(() -> new EntityNotFoundException("No active shift found for cashier ID: " + cashierId));

        LocalDateTime now = LocalDateTime.now();

        List<Order> orders = orderRepository.findByCashierIdAndCreatedAtBetween(
                cashierId, shift.getShiftStart(), now
        );

        List<Refund> refunds = refundRepository.findByCashierIdAndCreatedAtBetween(
                cashierId, shift.getShiftStart(), now
        );

        double totalRefunds = refunds.stream()
                .mapToDouble(r -> r.getAmount() != null ? r.getAmount() : 0.0)
                .sum();

        double totalSales = orders.stream()
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0)
                .sum();

        int totalOrders = orders.size();
        double netSales = totalSales - totalRefunds;

        double cashSales = orders.stream()
                .filter(o -> o.getPaymentType() == PaymentType.CASH)
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0)
                .sum();

        double cardSales = orders.stream()
                .filter(o -> o.getPaymentType() == PaymentType.CARD)
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0)
                .sum();

        double upiSales = orders.stream()
                .filter(o -> o.getPaymentType() == PaymentType.UPI)
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0)
                .sum();

        shift.setTotalSales(totalSales);
        shift.setTotalOrders(totalOrders);
        shift.setTotalRefunds(totalRefunds);
        shift.setNetSales(netSales);
        shift.setCashSales(cashSales);
        shift.setCardSales(cardSales);
        shift.setUpiSales(upiSales);
        shift.setRefunds(refunds);
        shift.setPaymentSummaries(getPaymentSummaries(orders, totalSales));
        shift.setRecentOrders(getRecentOrders(orders));

        return shift;
    }

    @Override
    public ShiftReport getShiftReportByCashierAndDate(Long cashierId, LocalDateTime date) {
        LocalDateTime start = date.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = date.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        ShiftReport shift = shiftReportRepository.findByCashierIdAndShiftStartBetween(cashierId, start, end)
                .orElseThrow(() -> new EntityNotFoundException("No shift report found on date: " + date));

        LocalDateTime shiftEnd = shift.getShiftEnd() != null ? shift.getShiftEnd() : end;
        List<Order> orders = orderRepository.findByCashierIdAndCreatedAtBetween(
                cashierId, shift.getShiftStart(), shiftEnd
        );
        shift.setPaymentSummaries(getPaymentSummaries(orders, shift.getTotalSales() != null ? shift.getTotalSales() : 0.0));
        shift.setRecentOrders(getRecentOrders(orders));

        return shift;
    }

    @Override
    public void deleteShiftReport(Long id) {
        if (!shiftReportRepository.existsById(id)) {
            throw new EntityNotFoundException("Shift report not found with ID: " + id);
        }
        shiftReportRepository.deleteById(id);
    }

    // ----------------- HELPER METHODS -----------------

    private List<PaymentSummary> getPaymentSummaries(List<Order> orders, double totalSales) {
        if (orders == null || orders.isEmpty()) {
            return new ArrayList<>();
        }

        Map<PaymentType, List<Order>> grouped = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getPaymentType() != null ?
                                order.getPaymentType() : PaymentType.CASH
                ));

        List<PaymentSummary> summaries = new ArrayList<>();

        for (Map.Entry<PaymentType, List<Order>> entry : grouped.entrySet()) {
            double amount = entry.getValue()
                    .stream()
                    .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0)
                    .sum();
            int transactions = entry.getValue().size();
            double percent = totalSales > 0 ? (amount / totalSales) * 100 : 0.0;

            PaymentSummary ps = new PaymentSummary();
            ps.setType(entry.getKey());
            ps.setTotalAmount(amount);
            ps.setTransactionCount(transactions);
            ps.setPercentage(percent);
            summaries.add(ps);
        }

        return summaries;
    }

    private List<Order> getRecentOrders(List<Order> orders) {
        if (orders == null) return new ArrayList<>();
        return orders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .collect(Collectors.toList());
    }
}
