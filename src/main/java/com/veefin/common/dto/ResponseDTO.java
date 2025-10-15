package com.veefin.common.dto;

import org.springframework.http.HttpStatus;

public class ResponseDTO {

    private Integer code;
    private String message;
    private Object data;

    public ResponseDTO(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
    public ResponseDTO(HttpStatus httpStatus, String message, Object data) {
        this.code = httpStatus.value();
        this.message = message;
        this.data = data;
    }
}
