package com.veefin.ap2.service;


import com.veefin.ap2.dto.IntentMandate;
import com.veefin.ap2.dto.PaymentMandate;
import com.veefin.ap2.dto.PaymentMandateContents;
import com.veefin.ap2.entity.PaymentMandateEntity;
import com.veefin.ap2.repository.PaymentMandateRepository;
import com.veefin.common.exception.ResourceNotFoundException;
import com.veefin.common.exception.ValidationException;
import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.service.InvoiceDataService;
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
    /**
     * Validate invoice vs intent, then create PaymentMandate.
     */
    public PaymentMandate createPaymentMandate(IntentMandate intent, String invoiceUuid) {

        if (!JwtSimulator.verifyAmountAndMerchant(
                intent.getUserAuthorization(),
                intent.getId(),
                intent.getMerchantName(),
                intent.getAmount())) {
            throw new ValidationException("JWT verification failed - merchant/amount mismatch or tampered data");
        }

        InvoiceData invoice = invoiceDataService.getInvoiceById(invoiceUuid);
        if (invoice == null) throw new ResourceNotFoundException("Invoice not found");

        // 1️Validate Merchant & Amount
        if (!intent.getMerchantName().equalsIgnoreCase(invoice.getMerchantName())) {
            throw new ValidationException("Merchant mismatch between Intent and Invoice");
        }

        if (intent.getAmount() < invoice.getTotalAmount()) {
            throw new ValidationException("Invoice amount exceeds approved intent amount");
        }

        // 2️⃣ AI validation reasoning (optional but cool)
//        String aiPrompt = String.format("""
//            You are an AI payment validator.
//            Review the following intent and invoice data.
//            Tell whether this payment should be authorized or not.
//
//            INTENT: %s
//            INVOICE: %s
//            """, intent.getNaturalLanguageDescription(), invoice.toString());

        String invoiceDescription = String.format(
                "Pay %s invoice %s for amount ₹%.2f due on %s",
                invoice.getMerchantName(),
                invoice.getInvoiceNumber(),
                invoice.getTotalAmount(),
                invoice.getDueDate()
        );
        String aiPrompt = String.format("""
Decide if the payment should be authorized.
Respond ONLY reason with short reason. with including APPROVE or REJECT.

Intent: %s
Invoice: %s
""", intent.getNaturalLanguageDescription(), invoiceDescription);
        String aiDecision =generateIntentSummary(aiPrompt);
        System.out.println(" AI Decision: " + aiDecision);
        System.err.println("-----------------------------------------------");

        System.err.println(invoiceDescription);

        System.err.println("-----------------------------------------------");
        System.err.println(intent.getNaturalLanguageDescription());


        if (aiDecision.toUpperCase().contains("REJECT")) {
            throw new ValidationException("AI validation failed: " + aiDecision);
        }

        // Create PaymentMandateContents
        PaymentMandateContents contents = new PaymentMandateContents();
        contents.setPaymentMandateId(UUID.randomUUID().toString());
        contents.setPaymentDetailsId(invoice.getUuid());
        contents.setMerchantAgent(invoice.getMerchantName());
        contents.setTotalAmount(invoice.getTotalAmount());
        contents.setCurrency("INR");
        contents.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        //  Simulate signing with JWT
        String simulatedJwt = JwtSimulator.sign(contents);

        // Build PaymentMandate
        PaymentMandate mandate = new PaymentMandate();
        mandate.setPaymentMandateContents(contents);
        mandate.setUserAuthorization(simulatedJwt);
        mandate.setAiValidation(aiDecision);

        // Persist to DB
        PaymentMandateEntity entity = new PaymentMandateEntity();
        entity.setPaymentMandateId(contents.getPaymentMandateId());
        entity.setMerchantName(invoice.getMerchantName());
        entity.setAmount(invoice.getTotalAmount());
        entity.setCurrency("INR");
        entity.setTimestamp(contents.getTimestamp());
        entity.setUserAuthorization(simulatedJwt);
        entity.setAiValidation(aiDecision);

        paymentRepo.save(entity);

        System.out.println("PaymentMandate Created and Stored Successfully");

        return mandate;
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
}
