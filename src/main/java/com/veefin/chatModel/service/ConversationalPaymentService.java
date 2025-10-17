package com.veefin.chatModel.service;

import com.veefin.ap2.entity.CartMandate;
import com.veefin.ap2.repository.CartMandateRepository;
import com.veefin.ap2.service.AP2Flow;
import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.enums.InvoiceStatus;
import com.veefin.invoice.service.InvoiceDataService;
import com.veefin.razorpay.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ConversationalPaymentService {

    private final ChatClient chatClient;
    private final AP2Flow ap2Flow;
    private final InvoiceDataService invoiceDataService;
    private final PaymentTransactionService paymentTransactionService;
    private final CartMandateRepository cartMandateRepository;


    public String processUserPrompt(String userPrompt) {
        try {


            if(userPrompt.contains("pay")) {
                String confirmationIntent = analyzeConfirmationIntent(userPrompt);
                if (confirmationIntent.equals("PROCEED")) {
                    return confirmPendingPayment(userPrompt); // Proceed with payment
                }
            }
            //
                // Step 1: Determine user intent using AI
//            String intentPrompt = String.format("""
//            Analyze this user message and determine the intent.
//            Return ONLY one of these: GET_INVOICES, PAY_INVOICE, GET_PAYMENT_HISTORY, UNKNOWN
//
//            User message: %s
//            """, userPrompt);



            String intentPrompt = String.format("""
                              Respond with **exactly one word**, no explanation or punctuation. Only choose from as per user message intent:
                             GET_INVOICES, PAY_INVOICE, GET_PAYMENT_HISTORY,UNKNOWN
                             User message: %s
                             """, userPrompt);

            String intent = Objects.requireNonNull(chatClient.prompt()
                            .user(intentPrompt)
                            .call()
                            .content())
                    .trim();

            // Step 2: Handle based on intent
            return switch (intent) {
                case "GET_INVOICES" -> invoiceDataService.getAllInvoicesResponse();
                case "GET_PAYMENT_HISTORY","PAYMENT_HISTORY" -> processPaymentHistoryRequest(userPrompt);
                default -> generateGeneralResponse(userPrompt);            };

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

    private String processPaymentHistoryRequest(String userPrompt) {
        try {
            // Extract specific invoice identifier if mentioned
//            String extractPrompt = String.format("""
//            Check if user is asking for payment history of a specific invoice.
//            If yes, extract the invoice identifier (number, merchant name, or ID).
//            If asking for all payment history, return "ALL".
//            If no specific invoice mentioned, return "ALL".
//
//            User request: %s
//            """, userPrompt);


            String extractPrompt = String.format("""
                  Return exactly one word/phrase:
                 - invoice identifier (number or merchant name) if specified
                 - otherwise return ALL as per user request intent.

User request: %s
""", userPrompt);



            String identifier = Objects.requireNonNull(chatClient.prompt()
                            .user(extractPrompt)
                            .call()
                            .content())
                    .trim();

            if ("ALL".equalsIgnoreCase(identifier)) {
                return paymentTransactionService.getAllPaymentHistory();
            } else {
                return paymentTransactionService.getPaymentHistoryForInvoice(identifier);
            }

        } catch (Exception e) {
            return "Failed to retrieve payment history: " + e.getMessage();
        }
    }


    public String processPaymentRequest( InvoiceData invoice) {
        try {
//            String extractPrompt = String.format("""
//                Extract the invoice identifier from this payment request.
//                Look for invoice number, merchant name, or invoice ID.
//                Return the exact identifier found, or "NOT_FOUND" if none.
//
//                User request: %s
//                """, userPrompt);
//
//            String identifier = Objects.requireNonNull(chatClient.prompt()
//                            .user(extractPrompt)
//                            .call()
//                            .content())
//                    .trim();
//
//            if ("NOT_FOUND".equals(identifier)) {
//                return "I couldn't identify which invoice to pay. Please specify invoice number, merchant name, or invoice ID.";
//            }
//
//            // Find invoice by identifier
//            InvoiceData invoice = invoiceDataService.findInvoiceByIdentifier(identifier);

//            if (invoice == null) {
//                return String.format(" Invoice '%s' not found. Use 'get all invoices' to see available invoices.", identifier);
//            }

            if(invoice.getStatus() == InvoiceStatus.PAID){
                return " Invoice already paid. Please check payment history.";
            }

            // 🧾 Create pending CartMandate
            CartMandate cart = getCartMandate(invoice);

            cartMandateRepository.save(cart);

            return String.format("""
                    🧾 Payment Summary:
                    
                    • Merchant: %s
                    • Invoice No: %s
                    • Amount: ₹%.2f
                    • Due Date: %s
                    • Payment Method: %s (%s)
                    
                    Please confirm payment (yes / no)
                    """,
                    invoice.getMerchantName(),
                    invoice.getInvoiceNumber(),
                    invoice.getTotalAmount(),
                    invoice.getDueDate(),
                    cart.getPaymentMethod(),
                    cart.getUpiId());

        } catch (Exception e) {
            return "⚠️ Failed to create payment request: " + e.getMessage();
        }
    }

    private static CartMandate getCartMandate(InvoiceData invoice) {
        CartMandate cart = new CartMandate();
        cart.setInvoiceUuid(invoice.getUuid());
        cart.setInvoiceNumber(invoice.getInvoiceNumber());
        cart.setFromMerchant(invoice.getMerchantName());
        cart.setToAccount("merchant@bank");
        cart.setAmount(invoice.getTotalAmount());
        cart.setCurrency("INR");
        cart.setPaymentMethod("UPI");
        cart.setUpiId("demo@paytm");
        cart.setDueDate(invoice.getDueDate());
        cart.setStatus("PENDING");
        cart.setUserSession("demo-user");
        return cart;
    }

    /**
     * User confirms payment → execute Razorpay simulation
     */
    private String confirmPendingPayment(String userPrompt) {


//        String extractPrompt = String.format("""
//                Extract the invoice identifier from this payment request.
//                Look for invoice number, merchant name, or invoice ID.
//                Return the exact identifier found, or "NOT_FOUND" if none as per user request intent.
//
//                User request: %s
//                """, userPrompt);


        String extractPrompt = String.format("""
           Extract the invoice identifier (number, merchant name, or ID) from the user's request.
           If no identifier is present, return "ALL"
           Respond with exactly one word or phrase
           User request: %s
           """, userPrompt);

        String identifier = Objects.requireNonNull(chatClient.prompt()
                        .user(extractPrompt)
                        .call()
                        .content())
                .trim();

//        if ("NOT_FOUND".equals(identifier)) {
//            return "I couldn't identify which invoice to pay. Please specify invoice number, merchant name, or invoice ID.";
//        }

        // Find invoice by identifier
        InvoiceData invoice = invoiceDataService.findInvoiceByIdentifier(identifier);

        CartMandate pendingCart = cartMandateRepository.findTopByInvoiceUuidAndStatusOrderByCreatedAtDesc(invoice.getUuid(),"PENDING");

        if (pendingCart == null) {
           return processPaymentRequest(invoice);
        }
        try {
            pendingCart.setStatus("CONFIRMED");
            cartMandateRepository.save(pendingCart);

            ap2Flow.executePaymentFlow(pendingCart.getInvoiceUuid());
            return String.format("""
                         ==================================================
                                          PAYMENT RECEIPT
                         ==================================================

                          Invoice Number : %s
                          Merchant       : %s
                          Amount Paid    : ₹%.2f
                          Payment Method : Razorpay
                          Status         : Completed
                          Date/Time      : %s

                        --------------------------------------------------
                        To view your transaction history, type:
                        "show payment history"
                        ==================================================
""",
                    pendingCart.getInvoiceNumber(),
                    pendingCart.getFromMerchant(),
                    pendingCart.getAmount(),
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"))
            );
        } catch (Exception e) {
            return " Payment failed: " + e.getMessage();
        }
    }

//    /**
//     * User cancels pending payment
//     */
//    private String cancelPendingPayment(String userPrompt) {
//        CartMandate pendingCart = cartMandateRepository.findTopByStatusOrderByCreatedAtDesc("PENDING");
//
//        if (pendingCart == null) {
//            return "No pending payment found to cancel.";
//        }
//
//        pendingCart.setStatus("CANCELLED");
//        cartMandateRepository.save(pendingCart);
//
//        return " Payment cancelled successfully for invoice " + pendingCart.getInvoiceNumber();
//    }



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





}