package com.veefin.ap2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntentMandate {
    private String id;
    private String naturalLanguageDescription;
    private String merchantName;
    private Double amount;
    private String currency;
    private String intentExpiry;
    private boolean requiresRefundability;
    private String userAuthorization; // Simulated JWT Signature
}