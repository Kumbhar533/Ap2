package com.veefin.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
