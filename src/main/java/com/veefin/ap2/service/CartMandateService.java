package com.veefin.ap2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veefin.ap2.entity.CartMandate;
import com.veefin.ap2.entity.IntentMandateEntity;
import com.veefin.ap2.repository.CartMandateRepository;
import com.veefin.ap2.repository.IntentMandateRepository;
import com.veefin.chatModel.service.PaymentProgressService;
import com.veefin.common.exception.ResourceNotFoundException;
import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.service.InvoiceDataService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartMandateService {

    private final CartMandateRepository cartRepo;
    private final IntentMandateRepository intentRepo;
    private final InvoiceDataService invoiceService;
    private final CryptographicService cryptoService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentProgressService paymentProgressService;


    private final AuditLogService auditService; // 🔧 ADD THIS

    /**
     * Backend creates Cart Mandate based on verified Intent
     * Backend signs this (NOT user)
     */
    public CartMandate createCartFromIntent(String intentHash, String sessionId) {
        IntentMandateEntity intent = null;
        String cartId = "PENDING";
        if (sessionId != null) {
            paymentProgressService.logStep(sessionId, "CART_START", "🔄 Starting Cart Mandate creation...", true);
        }
        try {
            // Find verified intent
            intent = intentRepo.findByIntentHash(intentHash);
            if (intent == null) {
                // 🔍 AUDIT: Intent not found
                auditService.logCartEvent("CREATE", cartId, null, "backend-agent", false,
                        "Intent not found for hash: " + intentHash, null, null);
                throw new ResourceNotFoundException("Intent not found");
            }

            //  AUDIT: Cart creation started
            auditService.logCartEvent("CREATE", cartId, intent.getInvoiceUuid(), "backend-agent", false,
                    "Cart creation started from intent", intent.getAmount(), intent.getMerchantName());

            //  Select eligible invoices based on intent rules
            List<InvoiceData> eligibleInvoices = selectEligibleInvoices(intent);

            // AUDIT: Invoice selection
            auditService.logCartEvent("VALIDATE", cartId, intent.getInvoiceUuid(), "backend-agent", true,
                    "Selected " + eligibleInvoices.size() + " eligible invoices", intent.getAmount(), intent.getMerchantName());

            //  Build Cart JSON
            CartJSON cartJson = buildCartJSON(intent, eligibleInvoices);
            cartId = cartJson.getCartId(); // Update cartId with actual value

            //  AUDIT: Cart JSON built
            auditService.logCartEvent("CREATE", cartId, intent.getInvoiceUuid(), "backend-agent", true,
                    "Cart JSON built successfully", cartJson.getTotal(), intent.getMerchantName());

            // 4 Backend signs the cart (NOT user)
            String cartJsonString = objectMapper.writeValueAsString(cartJson);
            String cartHash = cryptoService.computeSHA256(cartJsonString);
            String backendSignature = cryptoService.signWithAgentKey(cartJsonString);

            // 🔍 AUDIT: Cart signing
            auditService.logSignatureVerification(
                    com.veefin.ap2.entity.AP2AuditLog.MandateType.CART,
                    cartId,
                    "backend-agent",
                    true,
                    backendSignature.length() > 20 ? backendSignature.substring(0, 20) + "..." : backendSignature
            );

            //  Store Cart Mandate
            CartMandate cart = new CartMandate();
            cart.setUuid(UUID.randomUUID().toString());
            cart.setCartId(cartJson.getCartId());
            cart.setIntentHash(intentHash);
            cart.setInvoiceUuid(intent.getInvoiceUuid());
            cart.setCartHash(cartHash);
            cart.setCartJson(cartJsonString);
            cart.setBackendSignature(backendSignature);
            cart.setTotalAmount(cartJson.getTotal());
            cart.setStatus("CREATED");
            cart.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

            CartMandate savedCart = cartRepo.save(cart);
            if (sessionId != null) {
                paymentProgressService.logStep(sessionId, "CART_CREATED", "✅ Cart Mandate created successfully", true);
            }
            //  AUDIT: Cart creation successful
            auditService.logCartEvent("CREATE", cartId, intent.getInvoiceUuid(), "backend-agent", true,
                    "Cart mandate created and signed successfully", cartJson.getTotal(), intent.getMerchantName());

            return savedCart;

        } catch (Exception e) {
            //  AUDIT: Cart creation failed
            auditService.logCartEvent("CREATE", cartId,
                    intent != null ? intent.getInvoiceUuid() : null,
                    "backend-agent", false,
                    "Cart creation failed: " + e.getMessage(),
                    intent != null ? intent.getAmount() : null,
                    intent != null ? intent.getMerchantName() : null);
            throw new RuntimeException("Cart creation failed: " + e.getMessage());
        }
    }

    private List<InvoiceData> selectEligibleInvoices(IntentMandateEntity intent) {
        // Apply intent rules to select invoices
        return invoiceService.getAllInvoiceByMerchantName(intent.getMerchantName());
    }

    private CartJSON buildCartJSON(IntentMandateEntity intent, List<InvoiceData> invoices) {
        CartJSON cart = new CartJSON();
        cart.setCartId("cart-" + System.currentTimeMillis());
        cart.setIntentHash(intent.getIntentHash());
        cart.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        double total = 0;
        for (InvoiceData invoice : invoices) {
            CartJSON.InvoiceItem item = new CartJSON.InvoiceItem();
            item.setInvoiceId(invoice.getUuid());
            item.setAmount(invoice.getTotalAmount());
            cart.getInvoices().add(item);
            total += invoice.getTotalAmount();
        }
        cart.setTotal(total);

        return cart;
    }

    // Inner class for Cart JSON structure
    @Setter
    @Getter
    public static class CartJSON {
        private String cartId;
        private String intentHash;
        private List<InvoiceItem> invoices = new ArrayList<>();
        private double total;
        private String timestamp;

        // getters/setters...

        @Setter
        @Getter
        public static class InvoiceItem {
            private String invoiceId;
            private double amount;
            // getters/setters...
        }
    }
}