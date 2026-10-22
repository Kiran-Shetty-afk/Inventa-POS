package com.zosh.service;

import com.zosh.exception.ResourceNotFoundException;
import com.zosh.modal.SubscriptionPlan;

import java.util.List;

public interface SubscriptionPlanService {
    SubscriptionPlan createPlan(SubscriptionPlan plan);
    SubscriptionPlan updatePlan(Long id, SubscriptionPlan updatedPlan) throws ResourceNotFoundException;
    SubscriptionPlan getPlanById(Long id) throws ResourceNotFoundException;
    List<SubscriptionPlan> getAllPlans();
    void deletePlan(Long id) throws ResourceNotFoundException;
}
