package com.veefin.chat_model.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.veefin.chat_model.dto.UserRequest;
import com.veefin.chat_model.service.ConversationalPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AgentController {

    private final ConversationalPaymentService conversationalPaymentService;
    @PostMapping("/prompt")
    public String  createIntent(@RequestBody UserRequest userRequest) throws JsonProcessingException {

        return conversationalPaymentService.processUserPrompt(userRequest.getPrompt(), userRequest.getSession());
    }
}
