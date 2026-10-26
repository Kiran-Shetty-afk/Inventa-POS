package com.zosh.repository;

import com.zosh.modal.RefundItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RefundItemRepository extends JpaRepository<RefundItem, Long> {
    @Query("select coalesce(sum(ri.quantity), 0) from RefundItem ri where ri.orderItem.id = :orderItemId")
    Integer sumReservedQuantityByOrderItemId(@Param("orderItemId") Long orderItemId);

    @Query("select coalesce(sum(ri.quantity), 0) from RefundItem ri where ri.orderItem.order.id = :orderId")
    Integer sumReservedQuantityByOrderId(@Param("orderId") Long orderId);

    @Query("select ri from RefundItem ri join fetch ri.orderItem where ri.refund.id = :refundId")
    List<RefundItem> findByRefundIdWithOrderItem(@Param("refundId") Long refundId);
}
