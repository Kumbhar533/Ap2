package com.veefin.chat_model.service;

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
import com.veefin.chat_session.service.ChatHistoryService;
import com.veefin.common.exception.ResourceNotFoundException;
import com.veefin.common.utility.KeyGenerator;
import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.enums.InvoiceStatus;
import com.veefin.invoice.service.InvoiceDataService;
import com.veefin.payment_gateway.entity.model.Card;
import com.veefin.payment_gateway.service.CardService;
import com.veefin.payment_gateway.service.PaymentTransactionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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
    private final CardService cardService;

    private final ConcurrentHashMap<String, String> sessionInvoiceMemory = new ConcurrentHashMap<>();
    private final ChatHistoryService chatHistoryService;



    public String processUserPrompt(String userPrompt, String sessionId) {
        try {

             chatHistoryService.createOrGetSession(sessionId, "demo-user", userPrompt);
            // Single AI call to analyze all intents at once
            long startTime = System.currentTimeMillis();
            String combinedIntent = analyzeCombinedIntent(userPrompt);
            long endTime = System.currentTimeMillis();
            double timeTakenSeconds = (endTime - startTime) / 1000.0;
            log.info("ChatClient response time for combined intent: {} seconds", timeTakenSeconds);

            String response;
            log.info("Combined intent: {}", combinedIntent);
            switch (combinedIntent) {
                case "CARD_SELECTION" -> {
                    response = handleCardSelection(userPrompt, sessionId);
                }
                case "PAY_INVOICE" ,"PAY"->{
                    response =  handlePaymentIntent(userPrompt, sessionId);
                }
                case "SHOW_CARDS" -> {
                    response = getCardsDisplay();
                }
                case "PROCEED" -> {
                    response = showCardSelectionOptions(userPrompt, sessionId);
                }
                case "INVOICE_QUERY","INVOICE_ENQUIRY","INVOICE" -> {
                    response =  invoiceDataService.handleInvoiceQuery(userPrompt);
                }
                case "PAYMENT_HISTORY" ,"PAYMENT_TRANSACTION", "TRANSACTION","TRANSACTION_HISTORY"-> {
                    response =  paymentTransactionService.handlePaymentQuery(userPrompt);
                }
                case "CANCEL" -> {
                    response = handlePaymentCancellation(userPrompt, sessionId);
                }
                default -> {
                    response = generateGeneralResponse(userPrompt);
                }
            }
            chatHistoryService.saveMessage(sessionId, userPrompt, response);
            return response;

        } catch (Exception e) {
            log.error(" Error: {}", e.getMessage());
            return " Error: " + e.getMessage();
        }
    }



    private String confirmPendingPayment(String userPrompt, String maskedNumber, String sessionId) {
        try {
            String extractPrompt = String.format("""
Extract only the actual invoice identifier (for example: INV123, TXN-45, ACM-2025-001, etc.)
from the user's request below.

Rules:
1. If the user prompt mentions "card", "cards", or payment card terms 
   (like VISA, MasterCard, debit, credit, etc.) in any language — return exactly: NOT_FOUND
2. If the user prompt contains both a masked card number (e.g., ****1111, ending 4321) 
   AND an invoice identifier, return ONLY the invoice identifier.
3. If the user prompt contains ONLY masked card numbers (no invoice ID), return exactly: NOT_FOUND
4. Respond ONLY with the identifier itself (no extra text, no punctuation, no quotes).
5. If no valid identifier is found, return exactly: NOT_FOUND

User request: %s
""", userPrompt);


            String identifier = Objects.requireNonNull(chatClient.prompt()
                            .user(extractPrompt)
                            .call()
                            .content())
                    .trim();


            if (identifier.isBlank() || identifier.equalsIgnoreCase("NOT_FOUND") || identifier.equalsIgnoreCase("NOTFOUND")) {
                identifier = sessionInvoiceMemory.get(sessionId);
            }

            if (identifier == null) {
                return "No invoice in current session. Please start with 'pay invoice [number]'.";
            }
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

            Card cardDetails = cardService.getCardDetails(maskedNumber);
            // Execute payment via AP2Flow
            ap2Flow.executePaymentFlow(invoice.getUuid(), pendingCart,cardDetails.getProviderTokenId(), sessionId);

            return String.format("""
             **Payment Completed Successfully!**
            
             **Transaction Receipt:**
            • Invoice Number: %s
            • Merchant: %s
            • Amount Paid: ₹%.2f
            • Payment Method: CARD (PayPal)
            • Status:  COMPLETED
            • Date/Time: %s
            
              **Security Verified:**
               Intent validated
               Cart verified & signed
               Payment processed securely
            
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


    @Transactional
    private String handlePaymentIntent(String userPrompt, String sessionId) {
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



            if (identifier.isEmpty() || identifier.equalsIgnoreCase("NOT_FOUND")) {
                // fallback from memory
                identifier = sessionInvoiceMemory.get(sessionId);
            } else {
                // store in memory
                sessionInvoiceMemory.put(sessionId, identifier);
            }

            if (identifier == null) {
                return "No invoice found. Please specify an invoice number to start payment.";
            }
            // Find invoice
            InvoiceData invoice = invoiceDataService.findInvoiceByIdentifier(identifier);
            if (invoice == null) {
                return String.format("Invoice '%s' not found. Use 'get all invoices' to see available invoices.", identifier);
            }

            if (invoice.getStatus() == InvoiceStatus.PAID) {
                return "Invoice already paid. Please check payment history.";
            }

            return  preparePaymentAndAskConfirmation(invoice, userPrompt, sessionId);
        } catch (Exception e) {
            e.printStackTrace();
            return " Failed to process payment intent: " + e.getMessage();
        }


    }



    private String preparePaymentAndAskConfirmation(InvoiceData invoice, String userPrompt, String sessionId) throws JsonProcessingException {
        // Step 1: Create Intent Mandate
        createUserIntentMandate(invoice, userPrompt,sessionId);
        log.info("Intent Mandate created successfully");

        // Step 2: Create Cart from Intent
        handleCartCreation(userPrompt,invoice, sessionId);
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
    private void createUserIntentMandate(InvoiceData invoice, String userPrompt, String sessionId) throws JsonProcessingException {
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
                userSignature  ,             // String userSignature
                sessionId
        );

    }

    private void handleCartCreation(String userPrompt, InvoiceData invoice, String sessionId) {

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
        CartMandate cart = cartMandateService.createCartFromIntent(intentEntity.getIntentHash(), sessionId);
        cartMandateRepository.save(cart);
    }

    private IntentMandateEntity findIntentForInvoice(String invoiceUuid) {
        return intentMandateRepository.findByInvoiceUuidAndStatus(invoiceUuid,"CREATED");
    }



    private String showCardSelectionOptions(String userPrompt, String sessionId) {
        try {
            // Step 1 Extract invoice identifier using AI
            String extractPrompt = String.format("""
Extract only the actual invoice identifier (e.g., INV001, ACM-2025-001, TXN45, etc.)
from the user's request below.

Rules:
- Return ONLY the identifier itself (no extra words, punctuation, or formatting).
- If the user request does NOT clearly mention any identifier, return exactly the word: NOT_FOUND
- Do NOT guess or assume invoice numbers.

User request: %s
""", userPrompt);


            String identifier = Objects.requireNonNull(chatClient.prompt()
                            .user(extractPrompt)
                            .call()
                            .content())
                    .trim();

            if (identifier.isEmpty() || identifier.equalsIgnoreCase("NOTFOUND") || identifier.equalsIgnoreCase("NOT_FOUND")) {
                identifier = sessionInvoiceMemory.get(sessionId);
            }
            // Step 2️⃣ Find invoice
            InvoiceData invoice = invoiceDataService.findInvoiceByIdentifier(identifier);
            if (invoice == null) {
                return "Invoice not found. Please start with 'pay invoice [number]' first.";
            }

            // Step 3️⃣ Fetch user's saved cards from DB
            List<Card> savedCards = cardService.getAllUserCards("demo-user");
            if (savedCards == null || savedCards.isEmpty()) {
                return "No saved cards found for this user. Please add a card first.";
            }

            // Step 4️⃣ Format card list dynamically
            StringBuilder cardListBuilder = new StringBuilder();
            int index = 1;
            for (Card card : savedCards) {
                cardListBuilder.append(String.format(
                        "%d. **%s** - XXXX XXXX XXXX %s (%s)\n",
                        index++,
                        card.getCardAlias() != null ? card.getCardAlias() : "Card " + (index - 1),
                        card.getCardLast4(),
                        card.getCardNetwork() != null ? card.getCardNetwork() : "Unknown"
                ));
            }

            // Step 5️⃣ Final formatted message
            return String.format("""
             **Choose Payment Method for Invoice %s**

            **Available Cards:**
            %s

            **Invoice Details:**
            • Invoice: %s
            • Merchant: %s
            • Amount: ₹%.2f

            **Please choose:**
            - Type "use 1111 card" or "default" to use a specific card
            - Type "cancel" to cancel payment

            **Example:** "use 1111 card for %s"
            """,
                    identifier,
                    cardListBuilder.toString(),
                    invoice.getInvoiceNumber(),
                    invoice.getMerchantName(),
                    invoice.getTotalAmount(),
                    identifier
            );

        } catch (Exception e) {
            log.error("Error showing card options: {}", e.getMessage(), e);
            return "Error showing card options: " + e.getMessage();
        }
    }



    private String handleCardSelection(String userPrompt, String sessionId) {
        String cardChoice = userPrompt.toLowerCase().trim();

        if (cardChoice.contains("1111") || cardChoice.contains("default")) {
            return confirmPendingPayment(userPrompt, "1111", sessionId);
        } else if (cardChoice.contains("4444")) {
            return confirmPendingPayment(userPrompt, "4444", sessionId);
        } else if (cardChoice.contains("cancel")) {
            return "Payment cancelled. You can start a new payment anytime.";
        } else {
            return getCardsDisplay();
        }
    }


    private String getCardsDisplay() {
        List<Card> allUserCards = cardService.getAllUserCards("demo-user");

        if (allUserCards == null || allUserCards.isEmpty()) {
            return """
         **No saved cards found for this user.**
        You can add a card to continue with your payment.
        """;
        }

        StringBuilder cardDisplay = new StringBuilder();
        cardDisplay.append("""
      **Your Saved Cards**
    ───────────────────────────────
    """);

        int index = 1;
        for (Card card : allUserCards) {
            String alias = card.getCardAlias() != null ? card.getCardAlias() : "Card " + index;
            String network = card.getCardNetwork() != null ? card.getCardNetwork() : "Unknown";
            String type = card.getCardType() != null ? card.getCardType() : "Unknown";
            String status = card.getIsActive() ? "Active" : "Inactive";

            cardDisplay.append(String.format("""
        **%d️⃣ %s**
        •    Card: **XXXX XXXX XXXX %s**
        •    Network: %s
        •    Type: %s
        •    Status: %s
        ───────────────────────────────
        """, index++, alias, card.getCardLast4(), network, type, status));
        }

        return cardDisplay.toString();
    }




    private String handlePaymentCancellation(String userPrompt, String sessionId) {
        try {
            // Extract invoice identifier to clean up any pending carts
            String extractPrompt = String.format("""
        Extract only the actual invoice identifier (e.g. INV123, ACM-2025-001, etc.)
        from the user's request below.
        If no identifier found, return "NOT_FOUND"
        
        User request: %s
        """, userPrompt);

            String identifier = Objects.requireNonNull(chatClient.prompt()
                            .user(extractPrompt)
                            .call()
                            .content())
                    .trim();

            if (identifier.isEmpty() || identifier.equalsIgnoreCase("NOT_FOUND")) {
                identifier = sessionInvoiceMemory.get(sessionId);
            }

            if (!"NOT_FOUND".equals(identifier)) {
                // Find invoice and clean up pending cart
                InvoiceData invoice = invoiceDataService.findInvoiceByIdentifier(identifier);
                if (invoice != null) {
                    CartMandate pendingCart = cartMandateRepository.findTopByInvoiceUuidAndStatusOrderByCreatedAtDesc(
                            invoice.getUuid(), "CREATED"
                    );

                    IntentMandateEntity pendingIntent = intentMandateRepository.findByInvoiceUuidAndStatus(
                            invoice.getUuid(), "CREATED"
                    );

                    if (pendingCart != null) {
                        // Mark cart as cancelled
                        pendingCart.setStatus("CANCELLED");
                        cartMandateRepository.save(pendingCart);
                        log.info("Cart {} cancelled by user", pendingCart.getCartId());
                    }
                    if (pendingIntent != null) {
                        // Mark intent as cancelled
                        pendingIntent.setStatus("CANCELLED");
                        intentMandateRepository.save(pendingIntent);
                        log.info("Intent {} cancelled by user", pendingIntent.getIntentHash());
                    }

                    return String.format("""
                  **Payment Cancelled**
                
                  **Cancelled Payment:**
                • Invoice: %s
                • Merchant: %s
                • Amount: ₹%.2f
                
                  **Status:** Payment request has been cancelled successfully.
                
                 **What's next?**
                - You can start a new payment anytime by saying "pay invoice [number]"
                - Type "get all invoices" to see available invoices
                - Ask me anything else!
                """,
                            invoice.getInvoiceNumber(),
                            invoice.getMerchantName(),
                            invoice.getTotalAmount()
                    );
                }
            }

            // Generic cancellation message if no specific invoice found
            return """
           **Payment Cancelled**
        
        Your payment request has been cancelled successfully.
        
          **What's next?**
        - Start a new payment: "pay invoice [number]"
        - View invoices: "get all invoices"  
        - Check payment history: "show payment history"
        - Ask me anything else!
        """;

        } catch (Exception e) {
            log.error("Error handling payment cancellation: {}", e.getMessage());
            return "Payment cancelled. You can start a new payment anytime.";
        }
    }




    private String analyzeCombinedIntent(String userPrompt) {
        String prompt = String.format("""
Detect the user's intent (any language). Return ONLY ONE of these keywords — nothing else.

Rules:
- If the message talks about invoices, bills, dues, due dates, status, or contains invoice IDs (like INV-, ACM-, etc.), classify as INVOICE_ENQUIRY unless it clearly asks to "pay".
- If message contains "pay" or "payment" and refers to an invoice, bill, or amount — it's PAY_INVOICE
- If message only confirms or agrees (yes, ok, confirm, go ahead) without specifying invoice/payment — it's PROCEED.


PAY_INVOICE - wants to pay or make a payment
INVOICE_ENQUIRY - asks about invoice details, bills, dues, due dates, or invoice status
PAYMENT_TRANSACTION - asks about past/recent payments or transactions
SHOW_CARDS - wants to view or list saved cards
CARD_SELECTION - mentions or selects a specific card (e.g., 1111, Visa)
PROCEED - agrees or confirms to continue (yes pay, ok, confirm)
CANCEL - refuses, cancels, or stops (no, cancel, stop)
OTHER - anything else

Return only the keyword.

Message: %s
""", userPrompt);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return (response != null && !response.isEmpty())
                    ? response.trim()
                    : "OTHER";

        } catch (Exception e) {
            return "OTHER";
        }
    }


}
