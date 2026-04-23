package com.zosh.service;

import com.zosh.domain.PaymentStatus;
import com.zosh.domain.SubscriptionStatus;
import com.zosh.modal.Subscription;
import com.zosh.payload.dto.SubscriptionDTO;

import java.util.List;

public interface SubscriptionService {

    // 🆕 Create a new subscription for a store
    Subscription createSubscription(Long storeId,
                                    Long planId,
                                    String gateway,
                                    String transactionId
    );

    // 🔄 Upgrade the current active subscription
    Subscription upgradeSubscription(Long storeId,
                                     Long planId,
                                     String gateway, String transactionId);

    // ✅ Activate subscription (after payment success)
    Subscription activateSubscription(Long subscriptionId);

    // 🚫 Cancel a subscription manually
    Subscription cancelSubscription(Long subscriptionId);

    // ⏳ Expire subscriptions that passed end date
    void expirePastSubscriptions();

    // 🧾 Update payment status (after webhook or manual)
    Subscription updatePaymentStatus(Long subscriptionId, PaymentStatus status);

    // 📋 🔍 Get all or filtered subscriptions of a store (if status provided)
    List<SubscriptionDTO> getSubscriptionsByStore(Long storeId, SubscriptionStatus status); // combine active + history

    // 📦 📍 Get all or filtered subscriptions (for admin)
    List<SubscriptionDTO> getAllSubscriptions(SubscriptionStatus status); // null status = all

    // 📅 Get subscriptions expiring in next X days
    List<SubscriptionDTO> getExpiringSubscriptionsWithin(int days);

    // 📈 Count subscriptions by status
    Long countByStatus(SubscriptionStatus status);
}
