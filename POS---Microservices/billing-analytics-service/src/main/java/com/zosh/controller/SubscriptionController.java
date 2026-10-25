package com.zosh.controller;

import com.zosh.domain.SubscriptionStatus;
import com.zosh.client.UserOrgClient;
import com.zosh.modal.Subscription;
import com.zosh.payload.dto.SubscriptionDTO;
import com.zosh.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserOrgClient userOrgClient;

    // 🆕 Store subscribes to a plan
    @PostMapping("/subscribe")
    @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER')")
    public ResponseEntity<Subscription> createSubscription(
            @RequestParam Long storeId,
            @RequestParam(required = false) String storeName,
            @RequestParam Long planId,
            @RequestParam(defaultValue = "RAZORPAY") String gateway,
            @RequestParam(required = false) String transactionId) {

        requireStoreAccess(storeId);

        Subscription sub = subscriptionService.createSubscription(storeId, storeName, planId, gateway, transactionId);
        return ResponseEntity.ok(sub);
    }

    // Direct upgrades used to activate a plan before payment. Require the verified
    // payment-link flow instead so an unpaid upgrade cannot become active.
    @PostMapping("/upgrade")
    public ResponseEntity<Void> upgradePlan() {
        throw new ResponseStatusException(
                HttpStatus.GONE,
                "Start an upgrade through the verified Razorpay payment flow."
        );
    }

    // ✅ Activate a subscription
    @PutMapping("/{subscriptionId}/activate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> activateSubscription(@PathVariable Long subscriptionId) {
        throw new ResponseStatusException(
                HttpStatus.GONE,
                "Subscription activation is performed only after verified payment."
        );
    }

    // ❌ Cancel a subscription
    @PutMapping("/{subscriptionId}/cancel")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Subscription> cancelSubscription(@PathVariable Long subscriptionId) {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(subscriptionId));
    }

    // Payment status can only be changed by the verified provider callback.
    @PutMapping("/{subscriptionId}/payment-status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> updatePaymentStatus(@PathVariable Long subscriptionId) {
        throw new ResponseStatusException(
                HttpStatus.GONE,
                "Payment status is updated only after verified provider payment."
        );
    }

    // ⏳ Expire past subscriptions (scheduled or admin triggered)
    @PostMapping("/admin/expire")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> expirePastSubscriptions() {
        subscriptionService.expirePastSubscriptions();
        return ResponseEntity.ok("Past subscriptions expired successfully");
    }

    // 📦 Get all subscriptions for a store (optionally by status)
    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<SubscriptionDTO>> getStoreSubscriptions(
            @PathVariable Long storeId,
            @RequestParam(required = false) SubscriptionStatus status) {
        requireStoreAccess(storeId);
        return ResponseEntity.ok(subscriptionService.getSubscriptionsByStore(storeId, status));
    }

    // 🗂️ Admin: get all subscriptions
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<SubscriptionDTO>> getAllSubscriptions(
            @RequestParam(required = false) SubscriptionStatus status) {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions(status));
    }

    // ⌛ Admin: expiring subscriptions
    @GetMapping("/admin/expiring")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<SubscriptionDTO>> getExpiringSubscriptions(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(subscriptionService.getExpiringSubscriptionsWithin(days));
    }

    // 📊 Count by status
    @GetMapping("/admin/count")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Long> countByStatus(@RequestParam SubscriptionStatus status) {
        return ResponseEntity.ok(subscriptionService.countByStatus(status));
    }

    private void requireStoreAccess(Long storeId) {
        if (storeId == null || !Boolean.TRUE.equals(userOrgClient.canCurrentUserAccessStore(storeId))) {
            throw new AccessDeniedException("You are not authorized to access this store's subscription");
        }
    }
}
