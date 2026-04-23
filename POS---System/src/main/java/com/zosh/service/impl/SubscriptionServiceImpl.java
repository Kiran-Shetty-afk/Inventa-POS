package com.zosh.service.impl;


import com.zosh.domain.PaymentStatus;
import com.zosh.domain.SubscriptionStatus;
import com.zosh.mapper.SubscriptionMapper;
import com.zosh.modal.Store;
import com.zosh.modal.Subscription;
import com.zosh.modal.SubscriptionPlan;
import com.zosh.payload.dto.SubscriptionDTO;
import com.zosh.repository.StoreRepository;
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
    private final StoreRepository storeRepository;

    private final SubscriptionPlanRepository planRepository;

    private SubscriptionDTO toDto(Subscription subscription) {
        return SubscriptionMapper.toDto(subscription);
    }

    @Override
    public Subscription createSubscription(Long storeId,
                                           Long planId,
                                           String gateway,
                                           String transactionId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Subscription Plan not found"
                ));

        Subscription sub = Subscription.builder()
                .store(store)
                .plan(plan)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1)) // 🔁 use plan billing cycle in future
                .status(SubscriptionStatus.ACTIVE)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentGateway(gateway)
                .transactionId(transactionId)
                .build();
        return subscriptionRepository.save(sub);
    }

    @Override
    public Subscription upgradeSubscription(Long storeId,
                                            Long planId, String gateway,
                                            String transactionId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Subscription Plan not found"
                ));

        List<Subscription> activeSub=subscriptionRepository.findByStoreAndStatus(store,
                SubscriptionStatus.ACTIVE);

        for (Subscription sub : activeSub) {
            sub.setStatus(SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(sub);
        }


        Subscription sub = Subscription.builder()
                .store(store)
                .plan(plan)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status(SubscriptionStatus.ACTIVE)
                .paymentStatus(PaymentStatus.SUCCESS)
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
        sub.setPaymentStatus(PaymentStatus.SUCCESS);
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
        List<Subscription> all = subscriptionRepository.findAll();
        all.stream()
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
    public List<SubscriptionDTO> getSubscriptionsByStore(
            Long storeId,
            SubscriptionStatus status) {
        Store store = storeRepository.findById(storeId).orElseThrow(
                () -> new EntityNotFoundException("Store not found")
        );
        List<Subscription> subscriptions = status != null
                ? subscriptionRepository.findByStoreAndStatus(store, status)
                : subscriptionRepository.findByStore(store);
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
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Long countByStatus(SubscriptionStatus status) {
        return subscriptionRepository.countByStatus(status);
    }
}
