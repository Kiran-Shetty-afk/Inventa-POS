package com.zosh.modal;

import com.zosh.domain.ReservationState;
import com.zosh.domain.RestorationState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_workflow_steps", uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_workflow_product", columnNames = {"workflow_id", "product_id"}),
        @UniqueConstraint(name = "uk_order_reservation_operation", columnNames = "reservation_operation_key"),
        @UniqueConstraint(name = "uk_order_restoration_operation", columnNames = "restoration_operation_key")
})
@Getter
@Setter
@NoArgsConstructor
public class OrderWorkflowStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private OrderWorkflow workflow;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "reservation_operation_key", nullable = false, length = 128)
    private String reservationOperationKey;

    @Column(name = "restoration_operation_key", nullable = false, length = 128)
    private String restorationOperationKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_state", nullable = false, length = 16)
    private ReservationState reservationState = ReservationState.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "restoration_state", nullable = false, length = 16)
    private RestorationState restorationState = RestorationState.PENDING;
}
