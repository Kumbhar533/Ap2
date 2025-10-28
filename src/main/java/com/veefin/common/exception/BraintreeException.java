package com.veefin.common.exception;

public class BraintreeException  extends RuntimeException{
    public BraintreeException(String message) {
        super(message);
    }

    public BraintreeException(String message, Throwable cause) {
        super(message, cause);
    }
}
