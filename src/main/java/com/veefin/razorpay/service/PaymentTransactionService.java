package com.veefin.razorpay.service;

import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.service.InvoiceDataService;
import com.veefin.razorpay.entity.PaymentTransaction;
import com.veefin.razorpay.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final InvoiceDataService invoiceDataService;

    public String getAllPaymentHistory() {
        List<PaymentTransaction> payments = paymentRepository.findAll();

        if (payments.isEmpty()) {
            return "📋 No payment history found.";
        }

        StringBuilder response = new StringBuilder("💳 **Payment History:**\n\n");

        for (PaymentTransaction payment : payments) {
            InvoiceData invoice = invoiceDataService.getInvoiceById(payment.getInvoiceUuid());

            response.append(String.format("""
                 **Payment ID:** %s
                 **Invoice:** %s
                 **Merchant:** %s
                 **Amount:** ₹%.2f
                 **Method:** %s
                 **Status:** %s
                 **Date:** %s
                
                """,
                    payment.getRazorpayPaymentId(),
                    invoice != null ? invoice.getInvoiceNumber() : "Unknown",
                    invoice != null ? invoice.getMerchantName() : "Unknown",
                    payment.getAmount(),
                    payment.getPaymentMethod(),
                    payment.getStatus(),
                    payment.getCreatedAt().toString()
            ));
        }

        return response.toString();
    }

    public String getPaymentHistoryForInvoice(String identifier) {
        // Find invoice first
        InvoiceData invoice = invoiceDataService.findInvoiceByIdentifier(identifier);

        if (invoice == null) {
            return String.format("❌ Invoice '%s' not found.", identifier);
        }

        // Find payments for this invoice
        List<PaymentTransaction> payments = paymentRepository.findByInvoiceUuid(invoice.getUuid());

        if (payments == null) {
            return String.format("📋 No payment history found for invoice '%s'.", invoice.getInvoiceNumber());
        }

        StringBuilder response = new StringBuilder();
        response.append(String.format("💳 **Payment History for Invoice %s:**\n\n", invoice.getInvoiceNumber()));

        for (PaymentTransaction payment : payments) {
            response.append(String.format("""
                 **Payment ID:** %s
                 **Merchant:** %s
                 **Amount:** ₹%.2f
                 **Method:** %s
                 **Status:** %s
                 **Date:** %s
                 **Order ID:** %s
                
                """,
                    payment.getRazorpayPaymentId(),
                    invoice.getMerchantName(),
                    payment.getAmount(),
                    payment.getPaymentMethod(),
                    payment.getStatus(),
                    payment.getCreatedAt().toString(),
                    payment.getRazorpayOrderId()
            ));
        }

        return response.toString();
    }
}
