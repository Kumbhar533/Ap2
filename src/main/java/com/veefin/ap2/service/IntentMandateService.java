package com.veefin.ap2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veefin.ap2.dto.IntentMandate;
import com.veefin.ap2.entity.IntentMandateEntity;
import com.veefin.ap2.repository.IntentMandateRepository;
import com.veefin.chat_model.service.PaymentProgressService;
import com.veefin.common.exception.ResourceNotFoundException;
import com.veefin.common.exception.ValidationException;
import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.service.InvoiceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class IntentMandateService {

    private final IntentMandateRepository intentRepo;
    private final InvoiceDataService invoiceDataService;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CryptographicService cryptoService;
    private final AuditLogService auditService;
    private final PaymentProgressService paymentProgressService; // Add this

    public void createIntentForInvoice(String uuid, String userSignedIntentJSON, String intentHash, String userSignature, String sessionId) {

        if (sessionId != null) {
            paymentProgressService.logStep(sessionId, "INTENT_START", "Starting Intent Mandate creation...", true);
        }
        InvoiceData invoice = invoiceDataService.getInvoiceById(uuid);
        if (invoice == null) {
            auditService.logIntentEvent("CREATE", intentHash, uuid, "SYSTEM", false,
                    "Invoice not found", null, null);
            throw new ResourceNotFoundException("Invoice not found");
        }

        try {
            // 🔍 AUDIT: Intent creation started
            auditService.logIntentEvent("CREATE", intentHash, uuid, "demo-user", false,
                    "Intent creation started", invoice.getTotalAmount(), invoice.getMerchantName());

            // 2️⃣ Verify user's signature on Intent Mandate
            boolean verified = cryptoService.verifyUserSignature(userSignedIntentJSON, userSignature);

            // 🔍 AUDIT: Signature verification
            auditService.logSignatureVerification(
                    com.veefin.ap2.entity.AP2AuditLog.MandateType.INTENT,
                    intentHash,
                    "demo-user",
                    verified,
                    userSignature.length() > 20 ? userSignature.substring(0, 20) + "..." : userSignature
            );

            if (!verified) {
                auditService.logIntentEvent("CREATE", intentHash, uuid, "demo-user", false,
                        "User signature verification failed", invoice.getTotalAmount(), invoice.getMerchantName());
                throw new ValidationException("User signature verification failed");
            }

            // 3️⃣ Parse user's intent JSON
            IntentMandate mandate = objectMapper.readValue(userSignedIntentJSON, IntentMandate.class);
            auditService.logIntentEvent("VALIDATE", intentHash, uuid, "demo-user", true,
                    "User intent parsed successfully", invoice.getTotalAmount(), invoice.getMerchantName());

            // 4️⃣ Validate intent against invoice
            if (!mandate.getMerchantName().equals(invoice.getMerchantName()) ||
                    !mandate.getAmount().equals(invoice.getTotalAmount())) {

                auditService.logIntentEvent("VALIDATE", intentHash, uuid, "demo-user", false,
                        "Intent mismatch: merchant or amount", invoice.getTotalAmount(), invoice.getMerchantName());
                throw new ValidationException("Intent doesn't match invoice");
            }

            auditService.logIntentEvent("VALIDATE", intentHash, uuid, "demo-user", true,
                    "Intent matches invoice", invoice.getTotalAmount(), invoice.getMerchantName());

            // 5️⃣ Store intent with user's signature
            IntentMandateEntity entity = new IntentMandateEntity();
            entity.setInvoiceUuid(invoice.getUuid());
            entity.setMerchantName(invoice.getMerchantName());
            entity.setAmount(invoice.getTotalAmount());
            entity.setCurrency("INR");
            entity.setNaturalLanguageDescription(mandate.getNaturalLanguageDescription());
            entity.setIntentExpiry(mandate.getIntentExpiry());
            entity.setRequiresRefundability(false);
            entity.setStatus("CREATED");
            entity.setUserAuthorization(userSignature);
            entity.setIntentHash(intentHash);
            intentRepo.save(entity);

            if (sessionId != null) {
                paymentProgressService.logStep(sessionId, "INTENT_CREATED", "Intent Mandate created successfully", true);
            }
            // 🔍 AUDIT: Intent creation successful
            auditService.logIntentEvent("CREATE", intentHash, uuid, "demo-user", true,
                    "Intent mandate stored successfully", invoice.getTotalAmount(), invoice.getMerchantName());

        } catch (Exception e) {
            // 🔍 AUDIT: Intent creation failed
            auditService.logIntentEvent("CREATE", intentHash, uuid, "SYSTEM", false,
                    "Intent mandate processing failed: " + e.getMessage(),
                    invoice.getTotalAmount(),
                    invoice.getMerchantName());
            throw new ValidationException("Intent mandate processing failed: " + e.getMessage(), e);
        }
    }
}