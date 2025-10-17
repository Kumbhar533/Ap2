package com.veefin.ap2.service;

import com.veefin.ap2.dto.IntentMandate;
import com.veefin.ap2.dto.PaymentMandate;
import com.veefin.razorpay.service.RazorPayService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AP2Flow {


    private final IntentMandateService intentMandateService;

    private final RazorPayService razorPayService;
    private final PaymentMandateService paymentMandateService;


    @Transactional
    public void executePaymentFlow(String invoiceUuid) {
        // STEP 1: Generate Intent
        IntentMandate intent = intentMandateService.createIntentForInvoice(invoiceUuid);

        // STEP 2: Generate PaymentMandate (validates intent vs invoice)
        PaymentMandate paymentMandate = paymentMandateService.createPaymentMandate(intent, invoiceUuid);

        // STEP 3: AI-triggered payment processing
        try {
            String orderResponse = razorPayService.createOrder(
                    paymentMandate.getPaymentMandateContents().getTotalAmount(),
                    "INR",
                    paymentMandate.getPaymentMandateContents().getPaymentMandateId(),
                    invoiceUuid,intent.getMerchantName()
            );
            System.out.println("AI initiated payment: " + orderResponse);
        } catch (Exception e) {
            System.err.println(" AI payment failed: " + e.getMessage());
        }
    }




}