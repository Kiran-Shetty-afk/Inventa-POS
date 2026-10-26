package com.zosh.service;

import com.zosh.modal.Customer;
import com.zosh.modal.CustomerLoyaltyTransaction;
import com.zosh.modal.Order;
import com.zosh.modal.Refund;
import com.zosh.payload.dto.LoyaltyTransactionDTO;
import com.zosh.repository.CustomerLoyaltyTransactionRepository;
import com.zosh.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoyaltyPointsService {
    private final CustomerRepository customerRepository;
    private final CustomerLoyaltyTransactionRepository ledgerRepository;

    @Transactional
    public void creditForOrder(Order order) {
        if (order == null || order.getCustomer() == null || order.getCustomer().getId() == null) return;
        String key = "loyalty:order-earned:" + order.getId();
        if (ledgerRepository.findByIdempotencyKey(key).isPresent()) return;
        int earned = pointsFor(order.getTotalAmount());
        if (earned == 0) return;
        Customer customer = customerRepository.findByIdForUpdate(order.getCustomer().getId()).orElse(null);
        if (customer == null) return;
        int debt = nonNegative(customer.getLoyaltyPointsDebt());
        int offset = Math.min(earned, debt);
        int balanceDelta = earned - offset;
        int balance = Math.addExact(nonNegative(customer.getLoyaltyPoints()), balanceDelta);
        int remainingDebt = debt - offset;
        customer.setLoyaltyPoints(balance);
        customer.setLoyaltyPointsDebt(remainingDebt);
        customerRepository.save(customer);
        save(customer, order, null, "ORDER_EARN", balanceDelta, -offset, 0,
                balance, remainingDebt, key);
    }

    @Transactional
    public void reverseForConfirmedRefund(Order order, Refund refund, BigDecimal cumulativeConfirmedRefund) {
        if (order == null || refund == null || order.getCustomer() == null || order.getCustomer().getId() == null) return;
        String key = "loyalty:refund-reversal:" + refund.getId();
        if (ledgerRepository.findByIdempotencyKey(key).isPresent()) return;
        int earned = pointsFor(order.getTotalAmount());
        int priorReversed = ledgerRepository.sumReversedPointsByOrderId(order.getId());
        if (priorReversed < 0) priorReversed = 0;
        int target = proportionalPoints(earned, cumulativeConfirmedRefund, order.getTotalAmount());
        int reversal = Math.max(0, target - priorReversed);
        Customer customer = customerRepository.findByIdForUpdate(order.getCustomer().getId()).orElse(null);
        if (customer == null) return;
        int balance = nonNegative(customer.getLoyaltyPoints());
        int debt = nonNegative(customer.getLoyaltyPointsDebt());
        int deducted = Math.min(balance, reversal);
        int debtRaised = reversal - deducted;
        int nextBalance = balance - deducted;
        int nextDebt = Math.addExact(debt, debtRaised);
        customer.setLoyaltyPoints(nextBalance);
        customer.setLoyaltyPointsDebt(nextDebt);
        customerRepository.save(customer);
        save(customer, order, refund, "REFUND_REVERSAL", -deducted, debtRaised, reversal,
                nextBalance, nextDebt, key);
    }

    @Transactional
    public Customer addManualPoints(Long customerId, int points) {
        if (points <= 0) throw new IllegalArgumentException("Points must be greater than zero");
        Customer customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Customer not found"));
        int debt = nonNegative(customer.getLoyaltyPointsDebt());
        int offset = Math.min(points, debt);
        int balanceDelta = points - offset;
        int nextBalance = Math.addExact(nonNegative(customer.getLoyaltyPoints()), balanceDelta);
        int nextDebt = debt - offset;
        customer.setLoyaltyPoints(nextBalance);
        customer.setLoyaltyPointsDebt(nextDebt);
        customerRepository.save(customer);
        save(customer, null, null, "MANUAL_ADJUSTMENT", balanceDelta, -offset, 0,
                nextBalance, nextDebt, "loyalty:manual:" + UUID.randomUUID());
        return customer;
    }

    @Transactional(readOnly = true)
    public List<LoyaltyTransactionDTO> history(Long customerId) {
        return ledgerRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(entry -> LoyaltyTransactionDTO.builder().id(entry.getId())
                        .customerId(customerId)
                        .orderId(entry.getOrder() == null ? null : entry.getOrder().getId())
                        .refundId(entry.getRefund() == null ? null : entry.getRefund().getId())
                        .entryType(entry.getEntryType()).pointsDelta(entry.getPointsDelta())
                        .debtDelta(entry.getDebtDelta()).reversedPoints(entry.getReversedPoints())
                        .balanceAfter(entry.getBalanceAfter()).debtAfter(entry.getDebtAfter())
                        .createdAt(entry.getCreatedAt()).build()).toList();
    }

    private void save(Customer customer, Order order, Refund refund, String type,
                      int pointsDelta, int debtDelta, int reversed,
                      int balance, int debt, String key) {
        ledgerRepository.save(CustomerLoyaltyTransaction.builder().customer(customer).order(order)
                .refund(refund).entryType(type).pointsDelta(pointsDelta).debtDelta(debtDelta)
                .reversedPoints(reversed).balanceAfter(balance).debtAfter(debt)
                .idempotencyKey(key).createdAt(LocalDateTime.now()).build());
    }

    private int pointsFor(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return 0;
        return amount.divideToIntegralValue(BigDecimal.valueOf(100))
                .min(BigDecimal.valueOf(Integer.MAX_VALUE)).intValue();
    }

    private int proportionalPoints(int earned, BigDecimal refunded, BigDecimal orderTotal) {
        if (earned <= 0 || refunded == null || orderTotal == null || orderTotal.signum() <= 0) return 0;
        if (refunded.compareTo(orderTotal) >= 0) return earned;
        return BigDecimal.valueOf(earned).multiply(refunded).divide(orderTotal, 0, RoundingMode.DOWN).intValue();
    }

    private int nonNegative(Integer value) { return value == null || value < 0 ? 0 : value; }
}
