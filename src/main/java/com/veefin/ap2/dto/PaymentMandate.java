package com.veefin.ap2.dto;

import lombok.Data;

@Data
public class PaymentMandate {
    private PaymentMandateContents paymentMandateContents;
    private String backendSignature;  // Backend's signature (NOT user's)
    private String paymentHash;       // SHA256 of payment JSON
    private String status;            // CREATED, SENT_TO_GATEWAY, PROCESSED
    private String gatewayOrderId;
    private String gatewayPaymentId;
}