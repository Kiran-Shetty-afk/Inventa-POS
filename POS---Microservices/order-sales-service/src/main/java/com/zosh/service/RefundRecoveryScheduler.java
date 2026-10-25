package com.zosh.service;

import com.zosh.service.impl.RefundServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundRecoveryScheduler {
    private final RefundServiceImpl refundService;

    @Scheduled(fixedDelayString = "${orders.saga.recovery-interval-ms:30000}")
    public void recoverPendingRefunds() {
        refundService.recoverPendingRefunds();
    }
}
