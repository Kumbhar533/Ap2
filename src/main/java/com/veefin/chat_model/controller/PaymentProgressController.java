package com.veefin.chat_model.controller;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentProgressController {

    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping(value = "/logs/{sessionId}")
    public SseEmitter getPaymentLogs(@PathVariable String sessionId) {
        try {
            log.info("New client connected for session: {}", sessionId);
            SseEmitter emitter = new SseEmitter(300000L);

            emitters.put(sessionId, emitter);

            emitter.onCompletion(() -> emitters.remove(sessionId));
            emitter.onTimeout(() -> emitters.remove(sessionId));
            emitter.onError((ex) -> emitters.remove(sessionId));

            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Payment logs stream connected for session: " + sessionId));

            return emitter;
        } catch (Exception e) {
            log.error("SSE setup failed: {}", e.getMessage());
            SseEmitter errorEmitter = new SseEmitter();
            try {
                errorEmitter.send(SseEmitter.event()
                        .name("error")
                        .data("Connection failed: " + e.getMessage()));
                errorEmitter.complete();
            } catch (IOException ignored) {}
            return errorEmitter;
        }
    }

    public void sendLog(String sessionId, String step, String message, boolean success) {
        log.info("Payment Log [{}] {}: {} (success: {})", sessionId, step, message, success);

        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("log")
                        .data(String.format("{\"step\":\"%s\",\"message\":\"%s\",\"success\":%b,\"timestamp\":\"%s\"}",
                                step, message, success, java.time.LocalDateTime.now())));
            } catch (IOException e) {
                log.error("Failed to send log: {}", e.getMessage());
                emitters.remove(sessionId);
            }
        }
    }


    @PostMapping("/create")
    public Map<String, String> createSession() {
        String sessionId = "session_" + System.currentTimeMillis();
        return Map.of(
                "sessionId", sessionId,
                "status", "created",
                "logsUrl", "/api/payment/logs/" + sessionId
        );
    }


    @PreDestroy
    public void cleanup() {
        // Clean up all active SSE connections
        emitters.values().forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("Error closing SSE emitter", e);
            }
        });
        emitters.clear();
    }
}