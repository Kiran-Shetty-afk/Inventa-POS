package com.zosh.repository;

import com.zosh.domain.PaymentOrderStatus;
import com.zosh.modal.PaymentOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    List<PaymentOrder> findByUserId(Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select paymentOrder from PaymentOrder paymentOrder where paymentOrder.paymentLinkId = :paymentLinkId")
    Optional<PaymentOrder> findByPaymentLinkIdForUpdate(@Param("paymentLinkId") String paymentLinkId);
    List<PaymentOrder> findByStoreId(Long storeId);
    List<PaymentOrder> findByStatus(PaymentOrderStatus status);
}
