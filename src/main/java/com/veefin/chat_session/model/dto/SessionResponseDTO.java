package com.veefin.chat_session.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionResponseDTO {

    private String sessionId;
    private String label;
}
