package com.veefin.ap2.service;

import com.veefin.ap2.dto.IntentMandate;
import com.veefin.ap2.dto.PaymentMandateContents;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

public class JwtSimulator {

    public static String sign(IntentMandate mandate) {
        // In real AP2 → JOSE or VC-style signed JWT
        String header = Base64.getUrlEncoder()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

        String payload = String.format(
                "{\"merchant\":\"%s\",\"amount\":%.2f,\"exp\":\"%s\",\"timestamp\":\"%s\"}",
                mandate.getMerchantName(),
                mandate.getAmount(),
                mandate.getIntentExpiry(),
                LocalDateTime.now()
        );

        String encodedPayload = Base64.getUrlEncoder()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        // Simulated signature – in real life, generated with HMAC or RSA key
        String signature = Base64.getUrlEncoder()
                .encodeToString(("SIGN-" + mandate.getId()).getBytes(StandardCharsets.UTF_8));

        return String.join(".", header, encodedPayload, signature);
    }

    // 🔹 For PaymentMandateContents
    public static String sign(PaymentMandateContents contents) {
        String header = Base64.getUrlEncoder()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

        String payload = String.format(
                "{\"paymentMandateId\":\"%s\",\"merchant\":\"%s\",\"amount\":%.2f,\"currency\":\"%s\",\"timestamp\":\"%s\"}",
                contents.getPaymentMandateId(),
                contents.getMerchantAgent(),
                contents.getTotalAmount(),
                contents.getCurrency(),
                LocalDateTime.now()
        );

        String encodedPayload = Base64.getUrlEncoder()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        String signature = Base64.getUrlEncoder()
                .encodeToString(("SIGN-" + contents.getPaymentMandateId()).getBytes(StandardCharsets.UTF_8));

        return String.join(".", header, encodedPayload, signature);
    }


    // Add verification method
    public static boolean verify(String jwt, String expectedId) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) return false;

            String signature = parts[2];
            String expectedSignature = Base64.getUrlEncoder()
                    .encodeToString(("SIGN-" + expectedId).getBytes(StandardCharsets.UTF_8));

            return signature.equals(expectedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    public static String extractPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }


    public static boolean verifyAmountAndMerchant(String jwt, String expectedId, String expectedMerchant, Double expectedAmount) {
        try {
            // 1. Basic signature verification
            if (!verify(jwt, expectedId)) return false;

            // 2. Extract and parse payload
            String payload = extractPayload(jwt);
            if (payload == null) return false;

            // 3. Parse JSON payload (simple string parsing)
            boolean merchantMatch = payload.contains("\"merchant\":\"" + expectedMerchant + "\"");
            boolean amountMatch = payload.contains("\"amount\":" + String.format("%.2f", expectedAmount));

            return merchantMatch && amountMatch;

        } catch (Exception e) {
            return false;
        }
    }
}