package com.veefin.ap2.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cart_mandate")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartMandate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", length = 50, nullable = false, unique = true)
    private String uuid;

    @Column(name = "cart_id", unique = true)
    private String cartId;

    @Column(name = "intent_hash")
    private String intentHash; // Links back to Intent Mandate

    @Column(name = "invoice_uuid")
    private String invoiceUuid;

    @Column(name = "cart_hash")
    private String cartHash; // SHA256 of cart JSON

    @Column(name = "cart_json", columnDefinition = "TEXT")
    private String cartJson; // Full cart JSON

    @Column(name = "backend_signature", columnDefinition = "TEXT")
    private String backendSignature; // Backend's signature (NOT user's)

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "status")
    private String status; // CREATED, CONFIRMED, PROCESSED

    @Column(name = "timestamp")
    private String timestamp;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null || uuid.isEmpty()) {
            uuid = java.util.UUID.randomUUID().toString();
        }
    }

}