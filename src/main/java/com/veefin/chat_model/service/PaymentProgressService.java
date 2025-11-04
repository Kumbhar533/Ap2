package com.veefin.chat_model.service;

import com.veefin.chat_model.controller.PaymentProgressController;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentProgressService {

    private final PaymentProgressController progressController;

    public void logStep(String sessionId, String step, String message, boolean success) {
        progressController.sendLog(sessionId, step, message, success);
    }
}