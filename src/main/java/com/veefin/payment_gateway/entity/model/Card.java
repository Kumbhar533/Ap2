package com.veefin.payment_gateway.entity.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", length = 50, nullable = false, unique = true)
    private String uuid;

    /**
     * Your internal user reference (e.g. UUID or username)
     */
    @Column(name = "user_id", length = 64)
    private String userId;

    /**
     * Payment gateway name (razorpay, stripe, payu, etc.)
     */
    @Column(name = "provider_name",length = 64)
    private String providerName;

    /**
     * Customer ID as per the payment provider
     */
    @Column(name = "provider_customer_id",length = 64)
    private String providerCustomerId;

    /**
     * Tokenized ID from the payment provider (safe to store)
     */
    @Column(name = "provider_token_id",length = 128, unique = true)
    private String providerTokenId;

    /**
     * User-friendly alias (e.g. “My HDFC Credit Card”)
     */
    @Column(name = "card_alias",length = 128)
    private String cardAlias;

    /**
     * Name printed on card (optional, safe to store)
     */
    @Column(name = "card_holder_name", length = 128)
    private String cardHolderName;

    /**
     * Last 4 digits only
     */
    @Column(name = "card_last_4",nullable = false, length = 4)
    private String cardLast4;

    /**
     * Card network (Visa, Mastercard, RuPay, etc.)
     */
    @Column(name = "card_network",length = 32)
    private String cardNetwork;

    /**
     * Card type (credit, debit, prepaid, etc.)
     */
    @Column(name = "card_type", length = 16)
    private String cardType;

    /**
     * Optional: Expiry details
     */
    @Column(name = "expiry_month")
    private Integer expiryMonth;
    @Column(name = "expiry_year")
    private Integer expiryYear;


    /**
     * Marks whether this card is active
     */
    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null || uuid.isEmpty()) {
            uuid = java.util.UUID.randomUUID().toString();
        }
    }
}