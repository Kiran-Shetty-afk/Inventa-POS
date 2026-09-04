package com.zosh.service.impl;

import com.zosh.domain.PaymentStatus;
import com.zosh.domain.SubscriptionStatus;
import com.zosh.modal.Subscription;
import com.zosh.modal.SubscriptionPlan;
import com.zosh.payload.dto.SubscriptionDTO;
import com.zosh.repository.SubscriptionPlanRepository;
import com.zosh.repository.SubscriptionRepository;
import com.zosh.service.SubscriptionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;

    private SubscriptionDTO toDto(Subscription subscription) {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setId(subscription.getId());
        dto.setStoreId(subscription.getStoreId());
        dto.setStoreName(subscription.getStoreName());
        dto.setStartDate(subscription.getStartDate());
        dto.setEndDate(subscription.getEndDate());
        dto.setStatus(subscription.getStatus());
        dto.setPaymentStatus(subscription.getPaymentStatus());
        dto.setPaymentGateway(subscription.getPaymentGateway());
        dto.setTransactionId(subscription.getTransactionId());
        dto.setCreatedAt(subscription.getCreatedAt());
        dto.setUpdatedAt(subscription.getUpdatedAt());
        if (subscription.getPlan() != null) {
            dto.setPlanName(subscription.getPlan().getName());
            SubscriptionDTO.PlanSummaryDTO planSummary = SubscriptionDTO.PlanSummaryDTO.builder()
                    .id(subscription.getPlan().getId())
                    .name(subscription.getPlan().getName())
                    .description(subscription.getPlan().getDescription())
                    .build();
            dto.setPlan(planSummary);
        }
        SubscriptionDTO.StoreSummaryDTO storeSummary = SubscriptionDTO.StoreSummaryDTO.builder()
                .id(subscription.getStoreId())
                .brand(subscription.getStoreName())
                .build();
        dto.setStore(storeSummary);
        return dto;
    }

    @Override
    public Subscription createSubscription(Long storeId,
                                           String storeName,
                                           Long planId,
                                           String gateway,
                                           String transactionId) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription Plan not found"));

        Subscription sub = Subscription.builder()
                .storeId(storeId)
                .storeName(storeName)
                .plan(plan)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status(SubscriptionStatus.ACTIVE)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentGateway(gateway)
                .transactionId(transactionId)
                .build();
        return subscriptionRepository.save(sub);
    }

    @Override
    public Subscription upgradeSubscription(Long storeId, Long planId, String gateway, String transactionId) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription Plan not found"));

        List<Subscription> activeSubs = subscriptionRepository.findByStoreIdAndStatus(storeId, SubscriptionStatus.ACTIVE);
        for (Subscription sub : activeSubs) {
            sub.setStatus(SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(sub);
        }

        Subscription sub = Subscription.builder()
                .storeId(storeId)
                .plan(plan)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status(SubscriptionStatus.ACTIVE)
                .paymentStatus(PaymentStatus.PAID)
                .paymentGateway(gateway)
                .transactionId(transactionId)
                .build();
        return subscriptionRepository.save(sub);
    }

    @Override
    public Subscription activateSubscription(Long subscriptionId) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setPaymentStatus(PaymentStatus.PAID);
        return subscriptionRepository.save(sub);
    }

    @Override
    public Subscription cancelSubscription(Long subscriptionId) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        sub.setStatus(SubscriptionStatus.CANCELLED);
        return subscriptionRepository.save(sub);
    }

    @Override
    public void expirePastSubscriptions() {
        subscriptionRepository.findAll().stream()
                .filter(s -> s.getEndDate().isBefore(LocalDate.now()) && s.getStatus() != SubscriptionStatus.EXPIRED)
                .forEach(s -> {
                    s.setStatus(SubscriptionStatus.EXPIRED);
                    subscriptionRepository.save(s);
                });
    }

    @Override
    public Subscription updatePaymentStatus(Long subscriptionId, PaymentStatus status) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        sub.setPaymentStatus(status);
        return subscriptionRepository.save(sub);
    }

    @Override
    public List<SubscriptionDTO> getSubscriptionsByStore(Long storeId, SubscriptionStatus status) {
        List<Subscription> subscriptions = status != null
                ? subscriptionRepository.findByStoreIdAndStatus(storeId, status)
                : subscriptionRepository.findAllByStoreId(storeId);
        return subscriptions.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<SubscriptionDTO> getAllSubscriptions(SubscriptionStatus status) {
        List<Subscription> subscriptions = status != null
                ? subscriptionRepository.findByStatus(status)
                : subscriptionRepository.findAll();
        return subscriptions.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<SubscriptionDTO> getExpiringSubscriptionsWithin(int days) {
        LocalDate today = LocalDate.now();
        LocalDate future = today.plusDays(days);
        return subscriptionRepository.findByEndDateBetween(today, future)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public Long countByStatus(SubscriptionStatus status) {
        return subscriptionRepository.countByStatus(status);
    }
}
