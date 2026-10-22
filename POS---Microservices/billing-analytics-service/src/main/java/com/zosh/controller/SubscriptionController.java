package com.zosh.controller;

import com.zosh.domain.PaymentStatus;
import com.zosh.domain.SubscriptionStatus;
import com.zosh.modal.Subscription;
import com.zosh.payload.dto.SubscriptionDTO;
import com.zosh.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // 🆕 Store subscribes to a plan
    @PostMapping("/subscribe")
    public ResponseEntity<Subscription> createSubscription(
            @RequestParam Long storeId,
            @RequestParam(required = false) String storeName,
            @RequestParam Long planId,
            @RequestParam(defaultValue = "RAZORPAY") String gateway,
            @RequestParam(required = false) String transactionId) {

        Subscription sub = subscriptionService.createSubscription(storeId, storeName, planId, gateway, transactionId);
        return ResponseEntity.ok(sub);
    }

    // 🔁 Store upgrades to a new plan
    @PostMapping("/upgrade")
    public ResponseEntity<Subscription> upgradePlan(
            @RequestParam Long storeId,
            @RequestParam Long planId,
            @RequestParam(defaultValue = "RAZORPAY") String gateway,
            @RequestParam(required = false) String transactionId) {

        Subscription sub = subscriptionService.upgradeSubscription(storeId, planId, gateway, transactionId);
        return ResponseEntity.ok(sub);
    }

    // ✅ Activate a subscription
    @PutMapping("/{subscriptionId}/activate")
    public ResponseEntity<Subscription> activateSubscription(@PathVariable Long subscriptionId) {
        return ResponseEntity.ok(subscriptionService.activateSubscription(subscriptionId));
    }

    // ❌ Cancel a subscription
    @PutMapping("/{subscriptionId}/cancel")
    public ResponseEntity<Subscription> cancelSubscription(@PathVariable Long subscriptionId) {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(subscriptionId));
    }

    // 💳 Update payment status
    @PutMapping("/{subscriptionId}/payment-status")
    public ResponseEntity<Subscription> updatePaymentStatus(
            @PathVariable Long subscriptionId,
            @RequestParam PaymentStatus status) {
        return ResponseEntity.ok(subscriptionService.updatePaymentStatus(subscriptionId, status));
    }

    // ⏳ Expire past subscriptions (scheduled or admin triggered)
    @PostMapping("/admin/expire")
    public ResponseEntity<String> expirePastSubscriptions() {
        subscriptionService.expirePastSubscriptions();
        return ResponseEntity.ok("Past subscriptions expired successfully");
    }

    // 📦 Get all subscriptions for a store (optionally by status)
    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<SubscriptionDTO>> getStoreSubscriptions(
            @PathVariable Long storeId,
            @RequestParam(required = false) SubscriptionStatus status) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionsByStore(storeId, status));
    }

    // 🗂️ Admin: get all subscriptions
    @GetMapping("/admin")
    public ResponseEntity<List<SubscriptionDTO>> getAllSubscriptions(
            @RequestParam(required = false) SubscriptionStatus status) {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions(status));
    }

    // ⌛ Admin: expiring subscriptions
    @GetMapping("/admin/expiring")
    public ResponseEntity<List<SubscriptionDTO>> getExpiringSubscriptions(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(subscriptionService.getExpiringSubscriptionsWithin(days));
    }

    // 📊 Count by status
    @GetMapping("/admin/count")
    public ResponseEntity<Long> countByStatus(@RequestParam SubscriptionStatus status) {
        return ResponseEntity.ok(subscriptionService.countByStatus(status));
    }
}
