//package com.veefin.payment_gateway.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.razorpay.Order;
//import com.razorpay.RazorpayClient;
//import com.veefin.ap2.dto.PaymentMandate;
//import com.veefin.ap2.dto.PaymentMandateContents;
//import com.veefin.ap2.entity.PaymentMandateEntity;
//import com.veefin.ap2.service.CryptographicService;
//import com.veefin.ap2.service.PaymentMandateService;
//import com.veefin.invoice.entity.InvoiceData;
//import com.veefin.invoice.enums.InvoiceStatus;
//import com.veefin.invoice.repository.InvoiceRepository;
//import com.veefin.invoice.service.InvoiceVectorService;
//import com.veefin.payment_gateway.entity.model.PaymentTransaction;
//import com.veefin.payment_gateway.entity.dto.RazorPayWebHookDto;
//import com.veefin.payment_gateway.entity.dto.RazorpayWebhookEntityDto;
//import com.veefin.payment_gateway.repository.PaymentRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.codec.binary.Hex;
//import org.json.JSONObject;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import javax.crypto.Mac;
//import javax.crypto.spec.SecretKeySpec;
//import java.nio.charset.StandardCharsets;
//import java.security.InvalidKeyException;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class RazorPayService {
//
//    @Value("${razorpay.key_id}")
//    private String razorpayKeyId;
//
//    @Value("${razorpay.key_secret}")
//    private String razorpayKeySecret;
//
//    @Value("${razorpay.token_id}")
//    private String razorpayTokenId;
//
//    @Value("${razorpay.customer_id}")
//    private String razorpayCustomerId;
//
//
//    @Value(("${razorpay.webhook.secret}"))
//    private String webhookSecretKey;
//
//
//    private final CryptographicService cryptoService;
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    private final InvoiceRepository invoiceRepository;
//    private final PaymentRepository paymentRepository;
//    private final TransactionVectorStore transactionVectorStore;
//    private final InvoiceVectorService invoiceVectorService;
//    private final PaymentMandateService paymentMandateService;
//
//
//
//    public String createOrder(Double amount, String currency, String receipt, String invoiceUuid, String paymentMandateId) throws Exception {
//
//        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
//        JSONObject orderRequest = new JSONObject();
//        orderRequest.put("amount", amount*100);
//        orderRequest.put("currency", currency);
//        orderRequest.put("receipt", receipt);
//        orderRequest.put("method", "card");
//        orderRequest.put("customer_id", "cust_RXOWVC0hhwSL8d");
//        JSONObject notes = new JSONObject();
//        notes.put("invoice_uuid", invoiceUuid);
//        notes.put("payment_mandate_id", paymentMandateId);
//        orderRequest.put("notes", notes);
//        Order order = razorpay.orders.create(orderRequest);
//        String orderId = order.get("id");
//
//        log.info("Order created successfully. Order ID: {}", orderId);
//        // Auto-simulate UPI payment
//        //return simulateUpiPayment(orderId, amount, invoiceUuid, "");
////        createRecurringPayment(orderId, razorpayCustomerId, razorpayTokenId,amount);
//
//     //   initiatePaymentWithToken(orderId, razorpayTokenId, (int) (amount * 100), "nileshk@veefin.com", "9923199195", "123");
//        return orderId;
//    }
//
//
//
//
//
//public String simulateUpiPayment(String orderId, double amount, String invoiceUuid, String merchantName) {
//        try {
//            // Mock payment simulation (no actual Razorpay payment API call)
//            String mockPaymentId = "pay_mock_" + System.currentTimeMillis();
//
//            log.info("UPI Payment simulated successfully. Payment ID: {}", mockPaymentId);
//            log.info("UPI ID: demo@paytm");
//            log.info("Amount: ₹{} (TEST MODE)", amount);
//
//            // Store payment transaction
//            PaymentTransaction transaction = PaymentTransaction.builder()
//                    .invoiceUuid(invoiceUuid)
//                    .razorpayOrderId(orderId)
//                    .razorpayPaymentId(mockPaymentId)
//                    .amount(amount)
//                    .currency("INR")
//                    .paymentMethod("CARD")                    // Corrected from UPI
//                    .fromAccount("4111-XXXX-XXXX-1111")       // Mock card number or token reference
//                    .toAccount("merchant@razorpay")
//                    .fromAccountType("CARD")                   // Already correct
//                    .toAccountType("MERCHANT_ACCOUNT")
//                    .status("SUCCESS")
//                    .razorpayResponse("Mock card payment simulation successful")
//                    .build();
//
//            PaymentTransaction save = paymentRepository.save(transaction);
//
//            // store in vector
//            transactionVectorStore.storePaymentInVectorDB(save);
//            // Update invoice status to PAID
//            InvoiceData invoice = invoiceRepository.findByUuid(invoiceUuid);
//            if (invoice != null) {
//                invoice.setStatus(InvoiceStatus.PAID);
//                invoiceRepository.save(invoice);
//                log.info("Invoice {} marked as PAID", invoiceUuid);
//
//                // Update invoice in vector DB to reflect the new status
//                invoiceVectorService.updateInvoiceInVectorDB(invoice);
//                log.info("Invoice {} status updated in vector DB", invoiceUuid);
//            }
//
//            return "Payment successful: " + mockPaymentId;
//
//        } catch (Exception e) {
//            log.error("Payment simulation failed: {}", e.getMessage());
//
//            // Store failed transaction
//            PaymentTransaction failedTransaction = PaymentTransaction.builder()
//                    .invoiceUuid(invoiceUuid)
//                    .razorpayOrderId(orderId)
//                    .amount(amount)
//                    .status("FAILED")
//                    .razorpayResponse(e.getMessage())
//                    .build();
//            PaymentTransaction save = paymentRepository.save(failedTransaction);
//            transactionVectorStore.storePaymentInVectorDB(save);
//
//            return "Payment failed: " + e.getMessage();
//        }
//    }
//
//
//
//
//
//        public boolean verifyWebHookSignature(String payload, String signature) throws InvalidKeyException, NoSuchAlgorithmException {
//
//        SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecretKey.getBytes(StandardCharsets.UTF_8),"HmacSHA256");
//        Mac mac = Mac.getInstance("HmacSHA256");
//        mac.init(secretKeySpec);
//
//        byte[] hmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
//        String actualSignature = Hex.encodeHexString(hmac);
//
//        return MessageDigest.isEqual(actualSignature.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
//    }
//
//
//
//    public void handleRazorpayWebhook(String payload, String signature) throws Exception {
//        //  Verify Razorpay signature
//        if (!verifyWebHookSignature(payload, signature)) {
//            log.error("Invalid Razorpay webhook signature");
//            throw new SecurityException("Invalid Razorpay webhook signature");
//        }
//
//        //  Parse payload
//        RazorPayWebHookDto webhookDto = objectMapper.readValue(payload, RazorPayWebHookDto.class);
//        RazorpayWebhookEntityDto entity = webhookDto.getPayload().getPayment().getEntity();
//
//        // Extract payment details
//        String razorpayPaymentId = entity.getId();
//        String razorpayOrderId = entity.getOrder_id();
//        String invoiceUuid = entity.getNotes().get("invoice_uuid");
//        String paymentMandateId = entity.getNotes().get("payment_mandate_id");
//
//        log.info("Processing webhook for payment: {}, order: {}", razorpayPaymentId, razorpayOrderId);
//
//        //  Load PaymentMandate by ID
//        PaymentMandateEntity mandateEntity = paymentMandateService.getPaymentMandateById(paymentMandateId);
//        if (mandateEntity == null) {
//            throw new SecurityException("Payment mandate not found: " + paymentMandateId);
//        }
//
//        //  Verify backend signature on payment mandate
//        PaymentMandate mandate = convertToDto(mandateEntity);
//        boolean isValid = cryptoService.verifyAgentSignature(
//                objectMapper.writeValueAsString(mandate.getPaymentMandateContents()),
//                mandate.getBackendSignature()
//        );
//        if (!isValid) {
//            throw new SecurityException("Invalid AP2 payment mandate signature");
//        }
//
//        // Verify order ID matches mandate
//        if (!razorpayOrderId.equals(mandateEntity.getGatewayOrderId())) {
//            throw new SecurityException("Order ID mismatch in payment mandate");
//        }
//
//        //  Store payment transaction
//        PaymentTransaction transaction = PaymentTransaction.builder()
//                .invoiceUuid(invoiceUuid)
//                .razorpayOrderId(razorpayOrderId)
//                .razorpayPaymentId(razorpayPaymentId)
//                .amount(entity.getAmount() / 100.0) // Convert from paise to rupees
//                .currency("INR")
//                .paymentMethod(entity.getMethod())
//                .fromAccount("")
//                .toAccount("merchant@razorpay")
//                .fromAccountType("UPI")
//                .toAccountType("MERCHANT_ACCOUNT")
//                .status("SUCCESS")
//                .razorpayResponse("Webhook payment successful")
//                .build();
//
//        PaymentTransaction savedTransaction = paymentRepository.save(transaction);
//
//        // Store in vector DB
//        transactionVectorStore.storePaymentInVectorDB(savedTransaction);
//
//        // 7️⃣ Update invoice status
//        InvoiceData invoice = invoiceRepository.findByUuid(invoiceUuid);
//        if (invoice != null) {
//            invoice.setStatus(InvoiceStatus.PAID);
//            invoiceRepository.save(invoice);
//            log.info("Invoice {} marked as PAID", invoiceUuid);
//
//            // Update invoice in vector DB
//            invoiceVectorService.updateInvoiceInVectorDB(invoice);
//        }
//
//        // 8️⃣ Update payment mandate status
//        mandateEntity.setStatus("PROCESSED");
//        mandateEntity.setGatewayPaymentId(razorpayPaymentId);
//        paymentMandateService.updatePaymentMandateEntity(mandateEntity);
//
//        log.info("Webhook processed successfully for payment: {}", razorpayPaymentId);
//    }
//
//    private PaymentMandate convertToDto(PaymentMandateEntity entity) {
//        PaymentMandateContents contents = new PaymentMandateContents();
//        contents.setPaymentMandateId(entity.getPaymentMandateId());
//        contents.setCartId(entity.getCartId());
//        contents.setCartHash(entity.getCartHash());
//        contents.setTotalAmount(entity.getAmount());
//        contents.setCurrency(entity.getCurrency());
//        contents.setMerchantAgent(entity.getMerchantName());
//        contents.setPaymentMethod(entity.getPaymentMethod());
//        contents.setTimestamp(entity.getTimestamp());
//
//        PaymentMandate mandate = new PaymentMandate();
//        mandate.setPaymentMandateContents(contents);
//        mandate.setBackendSignature(entity.getBackendSignature());
//        mandate.setStatus(entity.getStatus());
//
//        return mandate;
//    }
//
//
//    //--------------------------------
//
//
//}
