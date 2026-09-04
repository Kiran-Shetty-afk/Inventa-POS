package com.zosh.modal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "fullName is mandatory")
    private String fullName;

    private String email;

    private String phone;

    private Long storeId;

    private Long branchId;

    @Column(nullable = false)
    @Builder.Default
    private Integer loyaltyPoints = 0;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void ensureDefaults() {
        if (loyaltyPoints == null) {
            loyaltyPoints = 0;
        }
    }

    @Transient
    public String getLoyaltyStatus() {
        int points = loyaltyPoints == null ? 0 : loyaltyPoints;
        if (points >= 500) {
            return "Gold";
        }
        if (points >= 200) {
            return "Silver";
        }
        if (points > 0) {
            return "Bronze";
        }
        return "None";
    }
}
