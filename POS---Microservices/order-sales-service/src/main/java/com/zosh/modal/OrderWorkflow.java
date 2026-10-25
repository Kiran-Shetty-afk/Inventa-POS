package com.zosh.modal;

import com.zosh.domain.OrderWorkflowState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_workflows")
@Getter
@Setter
@NoArgsConstructor
public class OrderWorkflow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderWorkflowState state;

    @Column(name = "failure_reason", length = 300)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<OrderWorkflowStep> steps = new ArrayList<>();

    public void addStep(OrderWorkflowStep step) {
        steps.add(step);
        step.setWorkflow(this);
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
