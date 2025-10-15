package com.veefin.ap2.dto;

import lombok.Data;

@Data
public class PaymentMandateContents {

    private String paymentMandateId;
    private String paymentDetailsId;
    private double totalAmount;
    private String currency;
    private String merchantAgent;
    private String timestamp;
}
