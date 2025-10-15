package com.veefin.ap2.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_mandate")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMandateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", length = 50, nullable = false, unique = true, updatable = false)
    private String uuid;

    @Column(name = "payment_mandate_id")
    private String paymentMandateId;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "timestamp")
    private String timestamp;

    @Column(name = "user_authorization", columnDefinition = "TEXT")
    private String userAuthorization;

    @Column(name = "ai_validation", columnDefinition = "TEXT")
    private String aiValidation;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null || uuid.isEmpty()) {
            uuid = java.util.UUID.randomUUID().toString();
        }
    }

}
