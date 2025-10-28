package com.veefin.ap2.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ap2_audit_log")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AP2AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mandate_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MandateType mandateType; // INTENT, CART, PAYMENT

    @Column(name = "mandate_id")
    private String mandateId; // Intent hash, Cart ID, Payment ID

    @Column(name = "invoice_uuid")
    private String invoiceUuid;

    @Column(name = "action", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditAction action; // CREATE, VERIFY, SIGN, PAY, FAIL

    @Column(name = "actor")
    private String actor; // userId, agentId, merchantId

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditStatus status; // SUCCESS, FAILURE

    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // JSON or descriptive text

    @Column(name = "signature_hash")
    private String signatureHash; // For signature verification events

    @Column(name = "amount")
    private Double amount;

    @Column(name = "merchant_name")
    private String merchantName;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    // Enums
    public enum MandateType {
        INTENT, CART, PAYMENT
    }

    public enum AuditAction {
        CREATE, VERIFY, SIGN, PAY, FAIL, VALIDATE
    }

    public enum AuditStatus {
        SUCCESS, FAILURE
    }
}