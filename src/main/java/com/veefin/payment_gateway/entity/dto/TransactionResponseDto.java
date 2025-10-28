package com.veefin.payment_gateway.entity.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class TransactionResponseDto {

    private String transactionId;
    private String currencyCode;
    private Double amount;
    private String paymentMethod;
    private String customerId;
    private String orderId;
}
