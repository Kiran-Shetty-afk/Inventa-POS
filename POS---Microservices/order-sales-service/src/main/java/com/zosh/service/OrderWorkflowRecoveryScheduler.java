package com.zosh.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderWorkflowRecoveryScheduler {
    private final OrderWorkflowCoordinator coordinator;
    private final OrderWorkflowProcessor processor;

    @Scheduled(fixedDelayString = "${orders.saga.recovery-interval-ms:30000}")
    public void recoverIncompleteWorkflows() {
        for (Long workflowId : coordinator.getRecoverableWorkflowIds()) {
            try {
                processor.processOrder(workflowId);
            } catch (RuntimeException exception) {
                // The persisted workflow and idempotency keys make the next scheduled retry safe.
                log.warn("Order workflow {} remains recoverable after a processing error", workflowId);
            }
        }
    }
}
