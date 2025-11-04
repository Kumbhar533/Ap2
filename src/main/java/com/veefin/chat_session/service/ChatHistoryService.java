package com.veefin.chat_session.service;


import com.veefin.chat_session.model.dto.ChatMessageDTO;
import com.veefin.chat_session.model.dto.SessionResponseDTO;
import com.veefin.chat_session.model.entity.ChatMessage;
import com.veefin.chat_session.model.entity.ChatSession;
import com.veefin.chat_session.repository.ChatMessageRepository;
import com.veefin.chat_session.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatSession createOrGetSession(String sessionId, String userId,String prompt) {
        Optional<ChatSession> existing = chatSessionRepository.findBySessionId(sessionId);

        if (existing.isPresent()) {
            return existing.get();
        }

        ChatSession newSession = new ChatSession();
        log.info("Creating new chat session for user: {}", userId);
        String label = Arrays.stream(prompt.split("\\s+"))
                .limit(5)                                // take first 5 words
                .collect(Collectors.joining(" "));       // join back with space
        newSession.setLabel(label);
        newSession.setSessionId(sessionId);
        newSession.setUserId(userId);

        return chatSessionRepository.save(newSession);
    }

    @Transactional
    public void saveMessage(String sessionId, String userMessage, String botResponse) {
        ChatSession session = chatSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        ChatMessage message = new ChatMessage();
        message.setChatSession(session);
        message.setUserMessage(userMessage);
        message.setBotResponse(botResponse);

        chatMessageRepository.save(message);
        log.info("Saved chat message for session: {}", sessionId);
    }

    public List<ChatMessageDTO> getChatHistory(String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAt(sessionId);
        List<ChatMessageDTO> dtoList = new ArrayList<>();

        for (ChatMessage msg : messages) {
            // Add user message
            if (msg.getUserMessage() != null && !msg.getUserMessage().isEmpty()) {
                dtoList.add(ChatMessageDTO.builder()
                        .id(msg.getId())
                        .content(msg.getUserMessage())
                        .role("USER")
                        .createdAt(msg.getCreatedAt())
                        .build());
            }

            // Add bot response
            if (msg.getBotResponse() != null && !msg.getBotResponse().isEmpty()) {
                dtoList.add(ChatMessageDTO.builder()
                        .id(msg.getId())
                        .content(msg.getBotResponse())
                        .role("BOT")
                        .createdAt(msg.getCreatedAt())
                        .build());
            }
        }

        return dtoList;
    }

    public List<SessionResponseDTO> getUserSessions(String userId) {
       List<ChatSession> sessions = chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);

       List<SessionResponseDTO> response = new ArrayList<>();
       for(ChatSession session : sessions) {
           response.add(SessionResponseDTO.builder()
                   .sessionId(session.getSessionId())
                   .label(session.getLabel())
                   .build());
       }
       return response;
    }

    @Transactional
    public void clearSession(String sessionId) {
        chatSessionRepository.deleteBySessionId(sessionId);
        log.info("Cleared session: {}", sessionId);
    }
}