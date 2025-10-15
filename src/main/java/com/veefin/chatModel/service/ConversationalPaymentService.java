package com.veefin.chatModel.service;

import com.veefin.ap2.entity.CartMandate;
import com.veefin.ap2.repository.CartMandateRepository;
import com.veefin.ap2.service.AP2Flow;
import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.enums.InvoiceStatus;
import com.veefin.invoice.repository.InvoiceRepository;
import com.veefin.invoice.service.InvoiceDataService;
import com.veefin.razorpay.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
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


            String confirmationIntent = analyzeConfirmationIntent(userPrompt);
            if (confirmationIntent.equals("PROCEED")) {
                return confirmPendingPayment(userPrompt); // Proceed with payment
            } else if (confirmationIntent.equals("CANCEL")) {
                return cancelPendingPayment(userPrompt); // Cancel the cart
            }
            // Step 1: Determine user intent using AI
            String intentPrompt = String.format("""
            Analyze this user message and determine the intent.
            Return ONLY one of these: GET_INVOICES, PAY_INVOICE, GET_PAYMENT_HISTORY, UNKNOWN
            
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
                case "PAY_INVOICE" -> processPaymentRequest(userPrompt);
                case "GET_PAYMENT_HISTORY" -> processPaymentHistoryRequest(userPrompt);
                default -> generateGeneralResponse(userPrompt);            };

        } catch (Exception e) {
            return " Error: " + e.getMessage();
        }
    }



    private String analyzeConfirmationIntent(String userPrompt) {
        String aiPrompt = String.format("""
        You are an AI assistant interpreting user confirmations.
        Determine if the user wants to PROCEED, CANCEL, or is UNCLEAR.

        Examples:
        - "yes", "ok", "go ahead" → PROCEED
        - "no", "cancel", "stop" → CANCEL
        - ambiguous text → UNCLEAR

        User message: "%s"
        Respond with ONLY one word: PROCEED, CANCEL, or UNCLEAR.
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
            String extractPrompt = String.format("""
            Check if user is asking for payment history of a specific invoice.
            If yes, extract the invoice identifier (number, merchant name, or ID).
            If asking for all payment history, return "ALL".
            If no specific invoice mentioned, return "ALL".
            
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


    public String processPaymentRequest(String userPrompt) {
        try {
            String extractPrompt = String.format("""
                Extract the invoice identifier from this payment request.
                Look for invoice number, merchant name, or invoice ID.
                Return the exact identifier found, or "NOT_FOUND" if none.
                
                User request: %s
                """, userPrompt);

            String identifier = Objects.requireNonNull(chatClient.prompt()
                            .user(extractPrompt)
                            .call()
                            .content())
                    .trim();

            if ("NOT_FOUND".equals(identifier)) {
                return "I couldn't identify which invoice to pay. Please specify invoice number, merchant name, or invoice ID.";
            }

            // Find invoice by identifier
            InvoiceData invoice = invoiceDataService.findInvoiceByIdentifier(identifier);

            if (invoice == null) {
                return String.format(" Invoice '%s' not found. Use 'get all invoices' to see available invoices.", identifier);
            }

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
        CartMandate pendingCart = cartMandateRepository.findTopByStatusOrderByCreatedAtDesc("PENDING");

        if (pendingCart == null) {
            return "No pending payment to confirm.";
        }
        try {
            pendingCart.setStatus("CONFIRMED");
            cartMandateRepository.save(pendingCart);

            ap2Flow.executePaymentFlow(pendingCart.getInvoiceUuid());
            return "Payment confirmed and processed successfully for invoice " + pendingCart.getInvoiceNumber();

        } catch (Exception e) {
            return " Payment failed: " + e.getMessage();
        }
    }

    /**
     * User cancels pending payment
     */
    private String cancelPendingPayment(String userPrompt) {
        CartMandate pendingCart = cartMandateRepository.findTopByStatusOrderByCreatedAtDesc("PENDING");

        if (pendingCart == null) {
            return "No pending payment found to cancel.";
        }

        pendingCart.setStatus("CANCELLED");
        cartMandateRepository.save(pendingCart);

        return " Payment cancelled successfully for invoice " + pendingCart.getInvoiceNumber();
    }



    private String generateGeneralResponse(String userPrompt) {
        String aiPrompt = String.format("""
        You are a friendly AI assistant.
        The user said: "%s"
        
        Respond naturally and helpfully — it could be any topic: tech, jokes, advice, or casual talk.
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