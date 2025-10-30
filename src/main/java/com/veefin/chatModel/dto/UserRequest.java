package com.veefin.chatModel.dto;

import lombok.Data;

@Data
public class UserRequest {

    private String prompt;
    private String session;
}
