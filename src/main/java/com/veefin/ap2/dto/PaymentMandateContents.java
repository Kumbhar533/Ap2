package com.veefin.ap2.dto;

import lombok.Data;

@Data
public class PaymentMandateContents {
    private String paymentMandateId;
    private String cartId;           // Links to Cart Mandate
    private String cartHash;         // SHA256 of cart JSON
    private double totalAmount;
    private String currency;
    private String merchantAgent;
    private String paymentMethod;    // UPI, CARD, etc.
    private String timestamp;
}