package com.veefin.chatModel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veefin.ap2.dto.IntentMandate;
import com.veefin.ap2.entity.CartMandate;
import com.veefin.ap2.entity.IntentMandateEntity;
import com.veefin.ap2.repository.CartMandateRepository;
import com.veefin.ap2.repository.IntentMandateRepository;
import com.veefin.ap2.service.AP2Flow;
import com.veefin.ap2.service.CartMandateService;
import com.veefin.ap2.service.IntentMandateService;
import com.veefin.common.exception.ResourceNotFoundException;
import com.veefin.common.utility.KeyGenerator;
import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.enums.InvoiceStatus;
import com.veefin.invoice.service.InvoiceDataService;
import com.veefin.invoice.service.InvoiceVectorService;
import com.veefin.payment_gateway.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationalPaymentService {

    private final ChatClient chatClient;
    private final AP2Flow ap2Flow;
    private final InvoiceDataService invoiceDataService;
    private final PaymentTransactionService paymentTransactionService;
    private final CartMandateRepository cartMandateRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KeyGenerator format;
    private final IntentMandateService intentMandateService;
    private final CartMandateService cartMandateService;
    private final IntentMandateRepository intentMandateRepository;

    private final InvoiceVectorService invoiceVectorService;

    public String processUserPrompt(String userPrompt) {
        try {
            if (userPrompt.toLowerCase().contains("yes") || userPrompt.toLowerCase().contains("confirm") || userPrompt.toLowerCase().contains("proceed")  || userPrompt.toLowerCase().contains("sure")) {
                String confirmationIntent = analyzeConfirmationIntent(userPrompt);
                if (confirmationIntent.equals("PROCEED")) {
                    return confirmPendingPayment(userPrompt);
                }
            }

            String intentPrompt = String.format("""
    Classify the user's message into **one keyword only** from:
    INVOICE_ENQUIRY
    PAYMENT_TRANSACTION
    PAY_INVOICE
    GENERAL

    Respond with exactly one of these keywords.

    Message: %s
""", userPrompt);

        log.info("user prompt :{}",userPrompt);
            long startTime = System.currentTimeMillis();
            String intent = Objects.requireNonNull(chatClient.prompt()
                            .user(intentPrompt)
                            .call()
                            .content())
                    .trim();

            long endTime = System.currentTimeMillis();
            double timeTakenSeconds = (endTime - startTime) / 1000.0;
            log.info("ChatClient response at intent time: {} seconds", timeTakenSeconds);

            log.info("User intent: {}", intent);

            return switch (intent) {
                case "INVOICE_QUERY","INVOICE_ENQUIRY","INVOICE" -> invoiceDataService.handleInvoiceQuery(userPrompt);
                case "PAY_INVOICE" ,"PAY"-> handlePaymentIntent(userPrompt);
                case "PAYMENT_HISTORY" ,"PAYMENT_TRANSACTION", "TRANSACTION","TRANSACTION_HISTORY"-> paymentTransactionService.handlePaymentQuery(userPrompt);
                default -> generateGeneralResponse(userPrompt);
            };

        } catch (Exception e) {
            return " Error: " + e.getMessage();
        }
    }


    private String analyzeConfirmationIntent(String userPrompt) {
        String aiPrompt = String.format("""
                You are an AI assistant interpreting user confirmations.
                Determine if the user wants to CONFIRM a payment or cancel it.
                
                Rules:
                - If the user clearly indicates agreement to pay (e.g., "yes please pay","yes pay", "confirm payment", "go ahead with payment"), respond with PROCEED.
                - If the user says no, cancel, or refuses payment, respond with CANCEL.
                - If the user is unclear or just saying 'yes' without referring to payment, respond with UNCLEAR.
                
                Respond with ONLY one word: PROCEED, CANCEL, or UNCLEAR.
                
                User message: "%s"
                """, userPrompt);
        try {
            String decision = Objects.requireNonNull(chatClient.prompt()
                            .user(aiPrompt)
                            .call()
                            .content())
                    .trim()
                    .toUpperCase();

            return switch (decision) {
                case "PROCEED" -> "PROCEED";
                case "CANCEL" -> "CANCEL";
                default -> "UNCLEAR";
            };
        } catch (Exception e) {
            return "UNCLEAR";
        }
    }


    private String confirmPendingPayment(String userPrompt) {
        try {
            String extractPrompt = String.format("""
    Extract only the actual invoice identifier (for example: INV123, TXN-45, etc.)
    from the user's request below.

    Do NOT respond with generic words like "invoice number", "merchant name", "id", or "invoice id".
    Respond ONLY with the identifier value itself, without extra text.

    User request: %s
    """, userPrompt);


            String identifier = Objects.requireNonNull(chatClient.prompt()
                            .user(extractPrompt)
                            .call()
                            .content())
                    .trim();

            // Find invoice by identifier
            InvoiceData invoice;

            invoice = invoiceDataService.findInvoiceByIdentifier(identifier);

            if (invoice == null) {
                return " Invoice not found. Please start with 'pay invoice [number]' first.";
            }

            // Look for cart with "CREATED" status (from CartMandateService)
            CartMandate pendingCart = cartMandateRepository.findTopByInvoiceUuidAndStatusOrderByCreatedAtDesc(
                    invoice.getUuid(), "CREATED"
            );

            if (pendingCart == null) {
                return "No prepared payment found. Please start with 'pay invoice [number]' first.";
            }

            // Update cart status to CONFIRMED and execute payment
            pendingCart.setStatus("CONFIRMED");
            cartMandateRepository.save(pendingCart);

            // Execute payment via AP2Flow
            ap2Flow.executePaymentFlow(invoice.getUuid(), pendingCart);

            return String.format("""
            🎉 **Payment Completed Successfully!**
            
            📋 **Transaction Receipt:**
            • Invoice Number: %s
            • Merchant: %s
            • Amount Paid: ₹%.2f
            • Payment Method: UPI (Razorpay)
            • Status: ✅ COMPLETED
            • Date/Time: %s
            
            🔐 **Security Verified:**
            ✅ Intent validated
            ✅ Cart verified & signed
            ✅ Payment processed securely
            
            --------------------------------------------------
            To view transaction history, type: "show payment history"
            ==================================================
            """,
                    invoice.getInvoiceNumber(),
                    invoice.getMerchantName(),
                    invoice.getTotalAmount(),
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"))
            );

        } catch (Exception e) {
            return " Payment execution failed: " + e.getMessage();
        }
    }




    private String generateGeneralResponse(String userPrompt) {
        String aiPrompt = String.format("""
                You are a friendly AI assistant.
                The user said: "%s"
                
                Respond naturally and helpfully — it could be any topic: tech, finance, advice, or casual talk.
                Keep your reply concise and conversational.
                """, userPrompt);

        try {
            String response = chatClient.prompt()
                    .user(aiPrompt)
                    .call()
                    .content();

            return (response != null && !response.isEmpty())
                    ? response.trim()
                    : "I'm here! What would you like to talk about?";
        } catch (Exception e) {
            return "Sorry, I couldn't process that right now.";
        }

    }


//-----------------------------------------------


    private String handlePaymentIntent(String userPrompt) {
        try {
            // Extract invoice identifier


            String extractPrompt = String.format("""
                Extract the invoice identifier (number, merchant name, or ID) from the user's request.
                If no identifier is present, return "NOT_FOUND"
                Respond with exactly one word or phrase
                User request: %s
                """, userPrompt);

            long startTime = System.currentTimeMillis();
            String identifier = Objects.requireNonNull(chatClient.prompt()
                            .user(extractPrompt)
                            .call()
                            .content())
                    .trim();
            long endTime = System.currentTimeMillis();
            double timeTakenSeconds = (endTime - startTime) / 1000.0;
            log.info("ChatClient response at payment intent time: {} seconds", timeTakenSeconds);

            log.info("Invoice identifier extracted: {}", identifier);



            if ("NOT_FOUND".equals(identifier)) {
                return "I couldn't identify which invoice to pay. Please specify invoice number, merchant name, or invoice ID.";
            }

            // Find invoice
            InvoiceData invoice = invoiceDataService.findInvoiceByIdentifier(identifier);
            if (invoice == null) {
                return String.format("Invoice '%s' not found. Use 'get all invoices' to see available invoices.", identifier);
            }

            if (invoice.getStatus() == InvoiceStatus.PAID) {
                return "Invoice already paid. Please check payment history.";
            }

            return  preparePaymentAndAskConfirmation(invoice, userPrompt);
        } catch (Exception e) {
            e.printStackTrace();
            return " Failed to process payment intent: " + e.getMessage();
        }


    }



    private String preparePaymentAndAskConfirmation(InvoiceData invoice, String userPrompt) throws JsonProcessingException {
            // Step 1: Create Intent Mandate
             createUserIntentMandate(invoice, userPrompt);
             log.info("Intent Mandate created successfully");

            // Step 2: Create Cart from Intent
             handleCartCreation(userPrompt,invoice);
             log.info("Cart created successfully");

            // Step 3: Show payment summary and ask for confirmation
            return String.format("""
             **Payment Ready for Confirmation**
            
             **Payment Details:**
            • Invoice: %s
            • Merchant: %s
            • Amount: ₹%.2f
            • Due Date: %s
            • Payment Method: CARD (PayPal)
            
             **Security Status:**
             Intent created & signed
             Cart created & verified
             Ready for payment execution
            
            **Do you want to proceed with this payment? (yes/no)**
            """,
                    invoice.getInvoiceNumber(),
                    invoice.getMerchantName(),
                    invoice.getTotalAmount(),
                    invoice.getDueDate()
            );
    }


    /**
     * 🔧 Step 2: Create Intent Mandate from user's payment request
     */
    private void createUserIntentMandate(InvoiceData invoice, String userPrompt) throws JsonProcessingException {
            // 1 Create Intent Mandate object
            IntentMandate intent = new IntentMandate();
            intent.setNaturalLanguageDescription(userPrompt); // User's exact words
            intent.setMerchantName(invoice.getMerchantName());
            intent.setAmount(invoice.getTotalAmount());
            intent.setCurrency("INR");
            intent.setIntentExpiry(LocalDateTime.now().plusHours(24).toString());
            intent.setRequiresRefundability(false);

            // 2️ Convert to JSON for signing
            String userSignedIntentJSON = objectMapper.writeValueAsString(intent);

            // 3️ Simulate user signature (in real app, user signs with private key)
            String userSignature = format.simulateUserSignature(userSignedIntentJSON);

            // 4️ Compute intent hash
            String intentHash = format.computeHash(userSignedIntentJSON);

            // 5️ Call existing IntentMandateService.createIntentForInvoice()
            intentMandateService.createIntentForInvoice(
                    invoice.getUuid(),          // String uuid
                    userSignedIntentJSON,       // String userSignedIntentJSON
                    intentHash,                 // String intentHash
                    userSignature               // String userSignature
            );

    }

    private void handleCartCreation(String userPrompt, InvoiceData invoice) {

//            // 1️ Extract invoice identifier from user prompt
//            String extractPrompt = String.format("""
//            Extract the invoice identifier (number, merchant name, or ID) from the user's request.
//            If no identifier is present, return "LATEST"
//            Respond with exactly one word or phrase
//            User request: %s
//            """, userPrompt);
//
//            String identifier = Objects.requireNonNull(chatClient.prompt()
//                            .user(extractPrompt)
//                            .call()
//                            .content())
//                    .trim();
//
//            // 2️ Find the latest intent for this invoice
//            InvoiceData invoice;
//            if ("LATEST".equals(identifier)) {
//                // Find most recent pending intent
//                invoice = findLatestPendingIntent();
//            } else {
//                invoice = invoiceDataService.findInvoiceByIdentifier(identifier);
//            }

            if (invoice == null) {
                throw new ResourceNotFoundException("Invoice not found");
            }

            // 3️ Find the intent mandate for this invoice
            IntentMandateEntity intentEntity = findIntentForInvoice(invoice.getUuid());
            if (intentEntity == null) {
                log.error("No payment intent found for invoice: {}", invoice.getUuid());
                throw new ResourceNotFoundException("No payment intent found for invoice: " + invoice.getUuid());
            }

            // 4️ Create Cart Mandate from Intent
            CartMandate cart = cartMandateService.createCartFromIntent(intentEntity.getIntentHash());
            cartMandateRepository.save(cart);
    }

    /**
     * 🔧 Find latest pending intent (helper method)
     */
    private InvoiceData findLatestPendingIntent() {
        // Implementation depends on your repository structure
        // For now, return null to force user to specify invoice
        return null;
    }

    /**
     * 🔧 Find intent mandate for invoice
     */
    private IntentMandateEntity findIntentForInvoice(String invoiceUuid) {
        return intentMandateRepository.findByInvoiceUuid(invoiceUuid);
    }


}
