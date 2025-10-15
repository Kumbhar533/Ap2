package com.veefin.ap2.dto;

import lombok.Data;

@Data
public class PaymentMandate {
    private PaymentMandateContents paymentMandateContents;
    private String userAuthorization;  // Simulated JWT
    private String aiValidation;       // AI’s comment or decision
}
