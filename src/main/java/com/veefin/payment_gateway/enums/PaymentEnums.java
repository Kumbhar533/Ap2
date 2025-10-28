package com.veefin.payment_gateway.enums;

public enum PaymentEnums {

    CREATED,
    FAILED,
    REVERSED,
    REFUNDED,
    PENDING,
    SUCCESS;


    public enum PaymentMethod {
        UPI,
        CARD,
        NET_BANKING,
        WALLET
    }
}


