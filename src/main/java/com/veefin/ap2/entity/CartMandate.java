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

    @Column(name = "invoice_uuid")
    private String invoiceUuid;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "from_merchant")
    private String fromMerchant;

    @Column(name = "to_account")
    private String toAccount;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "payment_method")
    private String paymentMethod; // UPI, CARD, etc.

    @Column(name = "upi_id")
    private String upiId;

    @Column(name = "due_date")
    private String dueDate;

    @Column(name = "status")
    private String status; // PENDING, CONFIRMED, CANCELLED

    @Column(name = "user_session")
    private String userSession; // To track which user's cart

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