package com.veefin.razorpay.service;

import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.enums.InvoiceStatus;
import com.veefin.invoice.repository.InvoiceRepository;
import com.veefin.razorpay.entity.PaymentTransaction;
import com.veefin.razorpay.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorPayService {

    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public String createOrder(Double amount, String currency, String receipt, String invoiceUuid, String merchantName) throws RazorpayException {
        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount*100);
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", receipt);
        Order order = razorpay.orders.create(orderRequest);
        String orderId = order.get("id");

        log.info("Order created successfully. Order ID: {}", orderId);

        // Auto-simulate UPI payment
        return simulateUpiPayment(orderId, amount, invoiceUuid, merchantName);
    }


    public String simulateUpiPayment(String orderId, double amount, String invoiceUuid, String merchantName) {
        try {
            // Mock payment simulation (no actual Razorpay payment API call)
            String mockPaymentId = "pay_mock_" + System.currentTimeMillis();

            log.info("UPI Payment simulated successfully. Payment ID: {}", mockPaymentId);
            log.info("UPI ID: demo@paytm");
            log.info("Amount: ₹{} (TEST MODE)", amount);

            // Store payment transaction
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .invoiceUuid(invoiceUuid)
                    .razorpayOrderId(orderId)
                    .razorpayPaymentId(mockPaymentId)
                    .amount(amount)
                    .currency("INR")
                    .paymentMethod("UPI")
                    .status("SUCCESS")
                    .razorpayResponse("Mock payment simulation successful")
                    .build();

            paymentRepository.save(transaction);
            // Update invoice status to PAID
            InvoiceData invoice = invoiceRepository.findByUuid(invoiceUuid);
            if (invoice != null) {
                invoice.setStatus(InvoiceStatus.PAID);
                invoiceRepository.save(invoice);
                log.info("Invoice {} marked as PAID", invoiceUuid);
            }

            return "Payment successful: " + mockPaymentId;

        } catch (Exception e) {
            log.error("Payment simulation failed: {}", e.getMessage());

            // Store failed transaction
            PaymentTransaction failedTransaction = PaymentTransaction.builder()
                    .invoiceUuid(invoiceUuid)
                    .razorpayOrderId(orderId)
                    .amount(amount)
                    .status("FAILED")
                    .razorpayResponse(e.getMessage())
                    .build();
            paymentRepository.save(failedTransaction);

            return "Payment failed: " + e.getMessage();
        }
    }



}
