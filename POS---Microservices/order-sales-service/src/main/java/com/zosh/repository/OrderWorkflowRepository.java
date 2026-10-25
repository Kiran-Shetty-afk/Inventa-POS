package com.zosh.repository;

import com.zosh.domain.OrderWorkflowState;
import com.zosh.modal.OrderWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderWorkflowRepository extends JpaRepository<OrderWorkflow, Long> {
    Optional<OrderWorkflow> findByOrderId(Long orderId);
    List<OrderWorkflow> findByStateIn(Collection<OrderWorkflowState> states);
}
