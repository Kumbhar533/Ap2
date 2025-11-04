package com.veefin.chat_session.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    
    private Long id;
    private String content;
    private String role; // "USER" or "BOT"
    private LocalDateTime createdAt;
}

