package com.zosh.service;

import com.zosh.domain.SubscriptionStatus;
import com.zosh.modal.Subscription;
import com.zosh.payload.dto.SubscriptionDTO;

import java.util.List;

public interface SubscriptionService {

    Subscription createSubscription(Long storeId,
                                    String storeName,
                                    Long planId,
                                    String gateway,
                                    String transactionId);

    Subscription completePaidUpgrade(Long storeId,
                                     Long planId,
                                     String gateway,
                                     String transactionId);

    Subscription cancelSubscription(Long subscriptionId);

    void expirePastSubscriptions();

    List<SubscriptionDTO> getSubscriptionsByStore(Long storeId, SubscriptionStatus status);

    List<SubscriptionDTO> getAllSubscriptions(SubscriptionStatus status);

    List<SubscriptionDTO> getExpiringSubscriptionsWithin(int days);

    Long countByStatus(SubscriptionStatus status);
}
