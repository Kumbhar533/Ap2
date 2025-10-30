package com.veefin.chatModel.service;

import com.veefin.chatModel.controller.PaymentProgressController;
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