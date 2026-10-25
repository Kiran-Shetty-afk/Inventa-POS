package com.zosh.controller;

import com.zosh.exception.ResourceNotFoundException;
import com.zosh.modal.SubscriptionPlan;
import com.zosh.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions/plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<SubscriptionPlan> createPlan(@RequestBody SubscriptionPlan plan) {
        return ResponseEntity.ok(subscriptionPlanService.createPlan(plan));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<SubscriptionPlan> updatePlan(
            @PathVariable Long id,
            @RequestBody SubscriptionPlan plan) throws ResourceNotFoundException {
        return ResponseEntity.ok(subscriptionPlanService.updatePlan(id, plan));
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionPlan>> getAllPlans() {
        return ResponseEntity.ok(subscriptionPlanService.getAllPlans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionPlan> getPlanById(@PathVariable Long id) throws ResourceNotFoundException {
        return ResponseEntity.ok(subscriptionPlanService.getPlanById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) throws ResourceNotFoundException {
        subscriptionPlanService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}
