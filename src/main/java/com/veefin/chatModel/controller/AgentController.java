package com.veefin.chatModel.controller;

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
    public String  createIntent(@RequestBody String prompt) {
        return conversationalPaymentService.processUserPrompt(prompt);
    }
}
