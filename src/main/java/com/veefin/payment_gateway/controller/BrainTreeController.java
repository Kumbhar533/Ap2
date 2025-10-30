package com.veefin.payment_gateway.controller;

import com.braintreegateway.*;
import com.veefin.payment_gateway.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class BrainTreeController {

    private final BraintreeGateway gateway;

    @Value("${braintree.customer-id}")
    private String customerId;

    private final PaymentTransactionService paymentTransactionService;
    // handle webhook

    @PostMapping("/webhook/braintree")
    public ResponseEntity<String> handleBraintreeWebhook(@RequestParam("bt_signature") String signature,
                                                         @RequestParam("bt_payload") String payload) {

        try {
            WebhookNotification notification = gateway.webhookNotification().parse(signature, payload);
            log.info("Webhook received: {}", notification.getKind());

            if (notification.getTransaction() != null) {
                paymentTransactionService.updatePaymentTransaction(notification);
            }
            return ResponseEntity.ok("Webhook received");
            } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage());
            return ResponseEntity.ok().body("Error processing webhook");
        }
    }




    // generate client token

    @GetMapping("/client-token/{customerId}")
    public String generateClientToken(@PathVariable String customerId) {
        ClientTokenRequest request = new ClientTokenRequest()
                .customerId(customerId); // Optional: pass null if no customer yet

        return gateway.clientToken().generate(request);
    }



    // create customer
    @PostMapping("/create-customer")
    public ResponseEntity<?> createCustomer(@RequestBody Map<String, String> payload) {
        String firstName = payload.get("firstName");
        String email = payload.get("email");

        CustomerRequest request = new CustomerRequest()
                .firstName(firstName)
                .email(email);

        Result<Customer> result = gateway.customer().create(request);

        if (result.isSuccess()) {
            String customerId = result.getTarget().getId();
            return ResponseEntity.ok(Map.of("customerId", customerId));
        } else {
            return ResponseEntity.status(400).body(Map.of("error", result.getMessage()));
        }
    }

    @PostMapping("/vault-payment-method")
    public ResponseEntity<?> vaultPaymentMethod(@RequestBody Map<String, String> payload) {
        String nonce = payload.get("nonce");


        PaymentMethodRequest request = new PaymentMethodRequest()
                .customerId(customerId)
                .paymentMethodNonce(nonce)
                .options()
                .makeDefault(true)
                .verifyCard(true)
                .done();

        Result<? extends PaymentMethod> result = gateway.paymentMethod().create(request);

        if (result.isSuccess()) {
            String token = result.getTarget().getToken();
            return ResponseEntity.ok(Map.of("token", token));
        } else {
            return ResponseEntity.status(400).body(Map.of("error", result.getMessage()));
        }
    }


}
