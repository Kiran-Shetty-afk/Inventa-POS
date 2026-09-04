package com.zosh.repository;

import com.zosh.domain.PaymentOrderStatus;
import com.zosh.modal.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    List<PaymentOrder> findByUserId(Long userId);
    Optional<PaymentOrder> findByPaymentLinkId(String paymentLinkId);
    List<PaymentOrder> findByStoreId(Long storeId);
    List<PaymentOrder> findByStatus(PaymentOrderStatus status);
}
