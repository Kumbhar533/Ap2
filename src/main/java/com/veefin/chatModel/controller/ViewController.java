package com.veefin.chatModel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "agent-prompt";
    }

    @GetMapping("/agent-ui")
    public String agentPromptPage() {
        return "agent-prompt";
    }
}

