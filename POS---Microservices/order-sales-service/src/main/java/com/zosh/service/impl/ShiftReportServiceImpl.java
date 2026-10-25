package com.zosh.service.impl;

import com.zosh.domain.PaymentType;
import com.zosh.exception.UserException;
import com.zosh.modal.*;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.RefundRepository;
import com.zosh.repository.ShiftReportRepository;
import com.zosh.payload.dto.TopSellingProductDTO;
import com.zosh.service.ShiftReportService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftReportServiceImpl implements ShiftReportService {

    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");

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
                .totalSales(ZERO_MONEY)
                .totalRefunds(ZERO_MONEY)
                .netSales(ZERO_MONEY)
                .totalOrders(0)
                .cashSales(ZERO_MONEY)
                .cardSales(ZERO_MONEY)
                .upiSales(ZERO_MONEY)
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

        BigDecimal totalRefunds = sumRefundAmounts(refunds);
        BigDecimal totalSales = sumOrderAmounts(orders);

        int totalOrders = orders.size();
        BigDecimal netSales = totalSales.subtract(totalRefunds);

        BigDecimal cashSales = sumOrderAmounts(orders.stream()
                .filter(o -> o.getPaymentType() == PaymentType.CASH).toList());
        BigDecimal cardSales = sumOrderAmounts(orders.stream()
                .filter(o -> o.getPaymentType() == PaymentType.CARD).toList());
        BigDecimal upiSales = sumOrderAmounts(orders.stream()
                .filter(o -> o.getPaymentType() == PaymentType.UPI).toList());

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
        shift.setTopSellingProducts(getTopSellingProducts(orders));

        return shiftReportRepository.save(shift);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftReport getShiftReportById(Long id) {
        ShiftReport shift = shiftReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shift report not found with ID: " + id));

        LocalDateTime end = shift.getShiftEnd() != null ? shift.getShiftEnd() : LocalDateTime.now();
        if (shift.getCashierId() != null && shift.getShiftStart() != null) {
            List<Order> orders = orderRepository.findByCashierIdAndCreatedAtBetween(
                    shift.getCashierId(), shift.getShiftStart(), end
            );
            shift.setPaymentSummaries(getPaymentSummaries(orders,
                    shift.getTotalSales() != null ? shift.getTotalSales() : ZERO_MONEY));
            shift.setRecentOrders(getRecentOrders(orders));
            shift.setTopSellingProducts(getTopSellingProducts(orders));
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
    @Transactional(readOnly = true)
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

        BigDecimal totalRefunds = sumRefundAmounts(refunds);
        BigDecimal totalSales = sumOrderAmounts(orders);

        int totalOrders = orders.size();
        BigDecimal netSales = totalSales.subtract(totalRefunds);

        BigDecimal cashSales = sumOrderAmounts(orders.stream()
                .filter(o -> o.getPaymentType() == PaymentType.CASH).toList());
        BigDecimal cardSales = sumOrderAmounts(orders.stream()
                .filter(o -> o.getPaymentType() == PaymentType.CARD).toList());
        BigDecimal upiSales = sumOrderAmounts(orders.stream()
                .filter(o -> o.getPaymentType() == PaymentType.UPI).toList());

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
        shift.setTopSellingProducts(getTopSellingProducts(orders));

        return shift;
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftReport getShiftReportByCashierAndDate(Long cashierId, LocalDateTime date) {
        LocalDateTime start = date.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = date.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        ShiftReport shift = shiftReportRepository.findByCashierIdAndShiftStartBetween(cashierId, start, end)
                .orElseThrow(() -> new EntityNotFoundException("No shift report found on date: " + date));

        LocalDateTime shiftEnd = shift.getShiftEnd() != null ? shift.getShiftEnd() : end;
        List<Order> orders = orderRepository.findByCashierIdAndCreatedAtBetween(
                cashierId, shift.getShiftStart(), shiftEnd
        );
        shift.setPaymentSummaries(getPaymentSummaries(orders,
                shift.getTotalSales() != null ? shift.getTotalSales() : ZERO_MONEY));
        shift.setRecentOrders(getRecentOrders(orders));
        shift.setTopSellingProducts(getTopSellingProducts(orders));

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

    private List<PaymentSummary> getPaymentSummaries(List<Order> orders, BigDecimal totalSales) {
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
            BigDecimal amount = sumOrderAmounts(entry.getValue());
            int transactions = entry.getValue().size();
            double percent = totalSales.signum() > 0
                    ? amount.multiply(BigDecimal.valueOf(100))
                            .divide(totalSales, 4, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

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

    private List<TopSellingProductDTO> getTopSellingProducts(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, ProductSales> salesByProduct = new HashMap<>();
        for (Order order : orders) {
            if (order.getItems() == null) {
                continue;
            }

            for (OrderItem item : order.getItems()) {
                if (item == null || item.getProductId() == null || item.getQuantity() == null
                        || item.getQuantity() <= 0) {
                    continue;
                }

                BigDecimal lineRevenue = item.getPrice() != null
                        ? item.getPrice()
                        : (item.getProductPrice() != null
                                ? item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                                : ZERO_MONEY);
                salesByProduct.computeIfAbsent(item.getProductId(), id -> new ProductSales(
                                id,
                                item.getProductName() != null && !item.getProductName().isBlank()
                                        ? item.getProductName()
                                        : "Unnamed product"))
                        .add(item.getQuantity(), lineRevenue);
            }
        }

        return salesByProduct.values().stream()
                .sorted(Comparator.comparingInt(ProductSales::getQuantity).reversed()
                        .thenComparing(ProductSales::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(5)
                .map(product -> TopSellingProductDTO.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .quantity(product.getQuantity())
                        .sellingPrice(product.getQuantity() > 0
                                ? product.getRevenue().divide(BigDecimal.valueOf(product.getQuantity()),
                                        2, RoundingMode.HALF_UP)
                                : ZERO_MONEY)
                        .build())
                .collect(Collectors.toList());
    }

    private static class ProductSales {
        private final Long id;
        private final String name;
        private int quantity;
        private BigDecimal revenue = ZERO_MONEY;

        private ProductSales(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        private void add(int soldQuantity, BigDecimal lineRevenue) {
            quantity += soldQuantity;
            revenue = revenue.add(lineRevenue);
        }

        private Long getId() {
            return id;
        }

        private String getName() {
            return name;
        }

        private int getQuantity() {
            return quantity;
        }

        private BigDecimal getRevenue() {
            return revenue;
        }
    }

    private BigDecimal sumOrderAmounts(List<Order> orders) {
        return orders.stream()
                .map(order -> order.getTotalAmount() == null ? ZERO_MONEY : order.getTotalAmount())
                .reduce(ZERO_MONEY, BigDecimal::add);
    }

    private BigDecimal sumRefundAmounts(List<Refund> refunds) {
        return refunds.stream()
                .map(refund -> refund.getAmount() == null ? ZERO_MONEY : refund.getAmount())
                .reduce(ZERO_MONEY, BigDecimal::add);
    }
}
