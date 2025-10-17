package com.veefin.ap2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veefin.ap2.dto.IntentMandate;
import com.veefin.ap2.entity.IntentMandateEntity;
import com.veefin.ap2.repository.IntentMandateRepository;
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

@RequiredArgsConstructor
@Service
public class IntentMandateService {

    private final IntentMandateRepository intentRepo;
    private final InvoiceDataService invoiceDataService;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IntentMandate createIntentForInvoice(String uuid) {
        // 1️⃣ Fetch invoice data
        InvoiceData invoice = invoiceDataService.getInvoiceById(uuid);
        if (invoice == null) throw new ResourceNotFoundException("Invoice not found");

        try {
            // 2️⃣ Prepare AI prompt
            String invoiceJson = objectMapper.writeValueAsString(invoice.getFieldMap());
//            String prompt = String.format("""
//                You are an autonomous payment agent following AP2 protocol.
//                Analyze this invoice JSON and generate a one-line human-readable summary
//                suitable for a payment intent mandate.
//
//                Invoice JSON:
//                %s
//                """, invoiceJson);

            // 3️⃣ Get AI-generated summary via Spring AI + OpenRouter
//            String description = chatClient
//                    .prompt()
//                    .user(prompt)
//                    .call()
//                    .content();

//            if (description == null || description.isBlank()) {
             String  description = String.format(
                        "Pay %s invoice %s for amount ₹%.2f due on %s",
                        invoice.getMerchantName(),
                        invoice.getInvoiceNumber(),
                        invoice.getTotalAmount(),
                        invoice.getDueDate()
                );
           // }

            // 4️⃣ Build IntentMandate DTO
            IntentMandate mandate = new IntentMandate();
            mandate.setId(UUID.randomUUID().toString());
            mandate.setNaturalLanguageDescription(description);
            mandate.setMerchantName(invoice.getMerchantName());
            mandate.setAmount(invoice.getTotalAmount());
            mandate.setCurrency("INR");
            mandate.setIntentExpiry(LocalDateTime.now().plusDays(7)
                    .format(DateTimeFormatter.ISO_DATE_TIME));
            mandate.setRequiresRefundability(false);

            // 5️⃣ Simulate signing (JWT)
            String simulatedJwt = JwtSimulator.sign(mandate);
            mandate.setUserAuthorization(simulatedJwt);

            // 6️⃣ Persist entity
            IntentMandateEntity entity = new IntentMandateEntity();
            entity.setInvoiceNumber(invoice.getInvoiceNumber());
            entity.setMerchantName(invoice.getMerchantName());
            entity.setAmount(invoice.getTotalAmount());
            entity.setCurrency("INR");
            entity.setNaturalLanguageDescription(description);
            entity.setIntentExpiry(mandate.getIntentExpiry());
            entity.setRequiresRefundability(false);
            entity.setUserAuthorization(simulatedJwt);
            intentRepo.save(entity);

            return mandate;

        } catch (Exception e) {
            throw new ValidationException("AI failed to create intent mandate: " + e.getMessage(), e);
        }
    }
}
