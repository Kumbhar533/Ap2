package com.veefin.ap2.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.veefin.ap2.dto.PaymentMandate;
import com.veefin.ap2.dto.PaymentMandateContents;
import com.veefin.ap2.entity.AP2AuditLog;
import com.veefin.ap2.entity.CartMandate;
import com.veefin.ap2.entity.IntentMandateEntity;
import com.veefin.ap2.entity.PaymentMandateEntity;
import com.veefin.ap2.repository.PaymentMandateRepository;
import com.veefin.common.exception.ResourceNotFoundException;
import com.veefin.common.exception.ValidationException;
import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.service.InvoiceDataService;
import com.veefin.payment_gateway.enums.PaymentEnums;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentMandateService {

    private final PaymentMandateRepository paymentRepo;
    private final ChatClient chatClient;
    private final InvoiceDataService invoiceDataService;
    private final CryptographicService cryptoService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuditLogService auditService;

    /**
     * Create PaymentMandate from existing Cart and Intent (for confirmed payments)
     */
    public PaymentMandate createPaymentMandateFromCart(CartMandate cart, IntentMandateEntity intent) {
        String paymentMandateId = "PENDING";

        try {
            // Get invoice data
            InvoiceData invoice = invoiceDataService.getInvoiceById(intent.getInvoiceUuid());
            if (invoice == null) {
                // 🔍 AUDIT: Invoice not found
                auditService.logPaymentEvent(AP2AuditLog.AuditAction.CREATE.name(), paymentMandateId,intent.getInvoiceUuid(), "backend-agent", false,
                        "Invoice not found for payment mandate creation", null, null);
                throw new ResourceNotFoundException("Invoice not found");
            }

            // 🔍 AUDIT: Payment mandate creation started
            auditService.logPaymentEvent(AP2AuditLog.AuditAction.CREATE.name(), paymentMandateId,intent.getInvoiceUuid(), "backend-agent", false,
                    "Payment mandate creation started", cart.getTotalAmount(), invoice.getMerchantName());

            // Verify cart signature
            boolean cartSignatureValid = cryptoService.verifyAgentSignature(cart.getCartJson(), cart.getBackendSignature());

            // 🔍 AUDIT: Cart signature verification
            auditService.logSignatureVerification(
                    com.veefin.ap2.entity.AP2AuditLog.MandateType.CART,
                    cart.getCartId(),
                    "backend-agent",
                    cartSignatureValid,
                    cart.getBackendSignature().length() > 20 ? cart.getBackendSignature().substring(0, 20) + "..." : cart.getBackendSignature()
            );

            if (!cartSignatureValid) {
                // 🔍 AUDIT: Cart signature verification failed
                auditService.logPaymentEvent(AP2AuditLog.AuditAction.VERIFY.name(), paymentMandateId,intent.getInvoiceUuid(), "backend-agent", false,
                        "Cart signature verification failed", cart.getTotalAmount(), invoice.getMerchantName());
                throw new ValidationException("Cart signature verification failed");
            }

            // AI validation (optional)
            String invoiceDescription = String.format(
                    "Pay %s invoice %s for amount ₹%.2f due on %s",
                    invoice.getMerchantName(),
                    invoice.getInvoiceNumber(),
                    invoice.getTotalAmount(),
                    invoice.getDueDate()
            );

            String aiPrompt = String.format("""
            Decide if the payment should be authorized.
            We always have sufficient funds.
            Respond ONLY with short reason including APPROVE or REJECT.

            Intent: %s
            Invoice: %s
            Cart Status: %s
            """, intent.getNaturalLanguageDescription(), invoiceDescription, cart.getStatus());

            String aiDecision = generateIntentSummary(aiPrompt);

            // 🔍 AUDIT: AI validation result
            auditService.logPaymentEvent(AP2AuditLog.AuditAction.VALIDATE.name(), paymentMandateId,intent.getInvoiceUuid(), "ai-agent",
                    !aiDecision.toUpperCase().contains("REJECT"),
                    "AI validation decision: " + aiDecision, cart.getTotalAmount(), invoice.getMerchantName());

            if (aiDecision.toUpperCase().contains("REJECT")) {
                // 🔍 AUDIT: AI validation failed
                auditService.logPaymentEvent(AP2AuditLog.AuditAction.VALIDATE.name(), paymentMandateId,intent.getInvoiceUuid(), "ai-agent", false,
                        "AI validation failed: " + aiDecision, cart.getTotalAmount(), invoice.getMerchantName());
                throw new ValidationException("AI validation failed: " + aiDecision);
            }

            // Create PaymentMandateContents
            PaymentMandateContents contents = new PaymentMandateContents();
            contents.setPaymentMandateId(UUID.randomUUID().toString());
            paymentMandateId = contents.getPaymentMandateId(); // Update with actual ID
            contents.setCartId(cart.getCartId());
            contents.setCartHash(cart.getCartHash());
            contents.setMerchantAgent(invoice.getMerchantName());
            contents.setTotalAmount(cart.getTotalAmount());
            contents.setCurrency("INR");
            contents.setPaymentMethod(PaymentEnums.PaymentMethod.CARD.name());
            contents.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

            // 🔍 AUDIT: Payment mandate contents created
            auditService.logPaymentEvent(AP2AuditLog.AuditAction.CREATE.name(), paymentMandateId,intent.getInvoiceUuid(), "backend-agent", true,
                    "Payment mandate contents created successfully", cart.getTotalAmount(), invoice.getMerchantName());

            // Backend signs the payment mandate
            String backendSignature = cryptoService.signWithAgentKey(
                    objectMapper.writeValueAsString(contents)
            );

            // 🔍 AUDIT: Payment mandate signing
            auditService.logSignatureVerification(
                    com.veefin.ap2.entity.AP2AuditLog.MandateType.PAYMENT,
                    paymentMandateId,
                    "backend-agent",
                    true,
                    backendSignature.length() > 20 ? backendSignature.substring(0, 20) + "..." : backendSignature
            );

            // Build PaymentMandate
            PaymentMandate mandate = new PaymentMandate();
            mandate.setPaymentMandateContents(contents);
            mandate.setBackendSignature(backendSignature);
            mandate.setStatus(PaymentEnums.CREATED.name());

            // Persist to DB
            PaymentMandateEntity entity = new PaymentMandateEntity();
            entity.setPaymentMandateId(contents.getPaymentMandateId());
            entity.setCartId(cart.getCartId());
            entity.setCartHash(cart.getCartHash());
            entity.setMerchantName(invoice.getMerchantName());
            entity.setAmount(cart.getTotalAmount());
            entity.setCurrency("INR");
            entity.setPaymentMethod(PaymentEnums.PaymentMethod.CARD.name());
            entity.setTimestamp(contents.getTimestamp());
            entity.setBackendSignature(backendSignature);
            entity.setStatus(PaymentEnums.CREATED.name());

            paymentRepo.save(entity);

            // 🔍 AUDIT: Payment mandate creation successful
            auditService.logPaymentEvent(AP2AuditLog.AuditAction.CREATE.name(), paymentMandateId,intent.getInvoiceUuid(), "backend-agent", true,
                    "Payment mandate created and stored successfully", cart.getTotalAmount(), invoice.getMerchantName());

            System.out.println("PaymentMandate created from Cart: " + cart.getCartId());
            return mandate;

        } catch (Exception e) {
            // 🔍 AUDIT: Payment mandate creation failed
            auditService.logPaymentEvent(AP2AuditLog.AuditAction.FAIL.name(), paymentMandateId,intent.getInvoiceUuid(), "backend-agent", false,
                    "Payment mandate creation from cart failed: " + e.getMessage(),
                    cart != null ? cart.getTotalAmount() : null,
                    intent.getMerchantName());
            throw new ValidationException("Payment mandate creation from cart failed: " + e.getMessage());
        }
    }


    /**
     * Sends a natural language prompt to OpenRouter (via Spring AI)
     * and returns a single-line response.
     */
    public String generateIntentSummary(String prompt) {
        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return (response != null && !response.isEmpty())
                    ? response.trim()
                    : "No summary generated";
        } catch (Exception e) {
            return "Error generating summary: " + e.getMessage();
        }
    }


    @Transactional
    public PaymentMandateEntity updatePaymentMandate(PaymentMandate mandate) {
        PaymentMandateEntity entity = paymentRepo.findByPaymentMandateId(
                mandate.getPaymentMandateContents().getPaymentMandateId()
        );
        if (entity != null) {
            entity.setStatus(mandate.getStatus());
            entity.setGatewayOrderId(mandate.getGatewayOrderId());
            entity.setGatewayPaymentId(mandate.getGatewayPaymentId());
            return paymentRepo.save(entity);
        }
        return null;
    }

    public PaymentMandateEntity getPaymentMandateById(String paymentMandateId) {
        return paymentRepo.findByPaymentMandateId(
                paymentMandateId);
    }

    public void updatePaymentMandateEntity(PaymentMandateEntity mandateEntity) {
        paymentRepo.save(mandateEntity);
    }
}
