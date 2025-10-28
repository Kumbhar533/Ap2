package com.veefin.payment_gateway.entity.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", length = 50, nullable = false, unique = true)
    private String uuid;

    @Column(name = "invoice_uuid")
    private String invoiceUuid;

    @Column(name = "transaction_id")
    private String transactionId;


    @Column(name = "amount")
    private Double amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "payment_method")
    private String paymentMethod; // UPI, CARD, etc.

    @Column(name = "from_account")
    private String fromAccount; // Payer's account (UPI ID, Card number, etc.)

    @Column(name = "to_account")
    private String toAccount; // Merchant's account

    @Column(name = "from_account_type")
    private String fromAccountType; // UPI, BANK_ACCOUNT, CARD

    @Column(name = "to_account_type")
    private String toAccountType; // MERCHANT_ACCOUNT, BANK_ACCOUNT

    @Column(name = "status")
    private String status; // SUCCESS, FAILED, PENDING

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
