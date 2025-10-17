package com.veefin.chatModel.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veefin.chatModel.service.ConversationalPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AgentController {

    private final ConversationalPaymentService conversationalPaymentService;
    @PostMapping("/prompt")
    public String  createIntent(@RequestBody String prompt) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.readTree(prompt);
        String message = node.get("prompt").asText();

        return conversationalPaymentService.processUserPrompt(message);
    }
}
