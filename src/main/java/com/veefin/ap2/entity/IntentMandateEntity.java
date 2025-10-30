package com.veefin.ap2.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "intent_mandate")
@AllArgsConstructor
@NoArgsConstructor
public class IntentMandateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", length = 50, nullable = false, unique = true, updatable = false)
    private String uuid;

    @Column(name = "intent_hash", unique = true)
    private String intentHash; // SHA256 of user's intent JSON

    @Column(name = "invoice_uuid")
    private String invoiceUuid;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "status")
    private String status;

    @Column(name = "total_amount")
    private Double amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "natural_language_description")
    private String naturalLanguageDescription;

    @Column(name = "intent_expiry")
    private String intentExpiry;

    @Column(name = "requires_refundability")
    private boolean requiresRefundability;

    @Column(name = "user_authorization", columnDefinition = "TEXT")
    private String userAuthorization; // USER'S signature (not backend's)

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