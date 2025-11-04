package com.veefin.chat_session.controller;


import com.veefin.chat_session.model.dto.ChatMessageDTO;
import com.veefin.chat_session.model.dto.SessionResponseDTO;
import com.veefin.chat_session.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(@PathVariable String sessionId) {
        List<ChatMessageDTO> history = chatHistoryService.getChatHistory(sessionId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponseDTO>> getUserSessions() {
        List<SessionResponseDTO> sessions = chatHistoryService.getUserSessions("demo-user");
        return ResponseEntity.ok(sessions);
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, String>> clearSession(@PathVariable String sessionId) {
        chatHistoryService.clearSession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Session cleared successfully"));
    }
}