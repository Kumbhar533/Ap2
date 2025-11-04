package com.veefin.chat_model.dto;

import lombok.Data;

@Data
public class UserRequest {

    private String prompt;
    private String session;
}
