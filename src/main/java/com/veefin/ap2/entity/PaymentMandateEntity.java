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

    @Column(name = "cart_id")
    private String cartId; // Links to Cart Mandate

    @Column(name = "cart_hash")
    private String cartHash; // SHA256 of cart JSON

    @Column(name = "payment_hash")
    private String paymentHash; // SHA256 of payment JSON

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "payment_method")
    private String paymentMethod; // UPI, CARD, etc.

    @Column(name = "timestamp")
    private String timestamp;

    @Column(name = "backend_signature", columnDefinition = "TEXT")
    private String backendSignature; // Backend's signature (NOT user's)

    @Column(name = "status")
    private String status; // CREATED, SENT_TO_GATEWAY, PROCESSED, FAILED

    @Column(name = "gateway_order_id")
    private String gatewayOrderId; // Razorpay order ID

    @Column(name = "gateway_payment_id")
    private String gatewayPaymentId; // Razorpay payment ID

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