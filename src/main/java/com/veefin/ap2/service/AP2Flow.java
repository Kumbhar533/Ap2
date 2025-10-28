package com.veefin.ap2.service;

import com.veefin.ap2.dto.PaymentMandate;
import com.veefin.ap2.entity.AP2AuditLog;
import com.veefin.ap2.entity.CartMandate;
import com.veefin.ap2.entity.IntentMandateEntity;
import com.veefin.ap2.repository.IntentMandateRepository;
import com.veefin.payment_gateway.entity.dto.TransactionResponseDto;
import com.veefin.payment_gateway.service.BrainTreeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AP2Flow {

    private final IntentMandateRepository intentMandateRepository;
    private final PaymentMandateService paymentMandateService;
    private final BrainTreeService brainTreeService;
    private final AuditLogService auditService;


    @Transactional
    public void executePaymentFlow(String invoiceUuid, CartMandate cart) {
        log.info("Starting payment flow for invoice: {} and cart: {}", invoiceUuid, cart.getCartId());

        //  Fetch intent from DB
        IntentMandateEntity intent = intentMandateRepository.findByIntentHash(cart.getIntentHash());
        if (intent == null) {
            log.error("No intent found for hash: {}", cart.getIntentHash());
            throw new IllegalArgumentException("Invalid intent reference for cart: " + cart.getCartId());
        }

        //  Create PaymentMandate (represents internal transaction record)
        PaymentMandate paymentMandate = paymentMandateService.createPaymentMandateFromCart(cart, intent);

        try {
            //  Trigger gateway payment creation
            log.info("Creating transaction in Braintree for amount: {}",
                    paymentMandate.getPaymentMandateContents().getTotalAmount());

            TransactionResponseDto txnResponse = brainTreeService.createTransaction(
                    paymentMandate.getPaymentMandateContents().getTotalAmount()
            );

            if (txnResponse.getTransactionId() != null) {
                // 5️ Handle post-payment success logic
                brainTreeService.processPaymentSuccess(
                        paymentMandate.getPaymentMandateContents().getPaymentMandateId(),
                        invoiceUuid,
                        txnResponse
                );
                auditService.logPaymentEvent(AP2AuditLog.AuditAction.PAY.name(), paymentMandate.getPaymentMandateContents().getPaymentMandateId(),
                        invoiceUuid, "backend-agent", true,
                        "Payment processed successfully", cart.getTotalAmount(), intent.getMerchantName());

                log.info("Payment flow completed successfully for invoice: {}", invoiceUuid);
            } else {
                auditService.logPaymentEvent(AP2AuditLog.AuditAction.FAIL.name(), paymentMandate.getPaymentMandateContents().getPaymentMandateId(),
                        invoiceUuid, "backend-agent", false,
                        "Payment failed", cart.getTotalAmount(), intent.getMerchantName());
                paymentMandate.setStatus("PAYMENT_FAILED");
                paymentMandateService.updatePaymentMandate(paymentMandate);
                log.warn("Payment failed for invoice: {} - no transactionId returned", invoiceUuid);
            }

        } catch (Exception e) {
            // 6️⃣ Handle any exception gracefully
            auditService.logPaymentEvent(AP2AuditLog.AuditAction.FAIL.name(), paymentMandate.getPaymentMandateContents().getPaymentMandateId(),
                    invoiceUuid, "backend-agent", false,
                    "Payment failed: " + e.getMessage(), cart.getTotalAmount(), intent.getMerchantName());
            log.error("Payment flow error for invoice {}: {}", invoiceUuid, e.getMessage(), e);
            paymentMandate.setStatus("FAILED");
            paymentMandateService.updatePaymentMandate(paymentMandate);
            throw new RuntimeException("AI payment failed for invoice: " + invoiceUuid, e);
        }
    }
}
