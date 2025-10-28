//package com.veefin.payment_gateway.controller;
//
//import com.veefin.payment_gateway.service.RazorPayService;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.codec.binary.Hex;
//import org.springframework.http.*;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.client.RestTemplate;
//
//import javax.crypto.Mac;
//import javax.crypto.spec.SecretKeySpec;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//import java.util.Map;
//
//@RequestMapping("/api/razorpay")
//@RestController
//@RequiredArgsConstructor
//@Slf4j
//public class RazorpayController {
//
//    private final RazorPayService razorPayService;
//
//    @PostMapping("/webhook")
//    public ResponseEntity<String> handleWebHook(
//            @RequestHeader("X-Razorpay-Signature") String signature,
//            HttpServletRequest httpServletRequest) throws IOException {
//
//        String payload = new String(httpServletRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
//        try {
//            System.out.println("Webhook payload: " + payload);
//            razorPayService.handleRazorpayWebhook(payload, signature);
//        } catch (Exception e) {
//            // Log the exception for internal debugging
//            e.printStackTrace();
//            // Do NOT throw exception back to Razorpay
//        }
//        System.out.println("Webhook signature: " + signature);
//        // Always return 200 OK to stop retries
//        return ResponseEntity.ok("Webhook received");
//    }
//
//
//
//    @PostMapping("/razorpay/verify")
//    public ResponseEntity<String> verifyPayment(@RequestBody Map<String, String> payload) throws Exception {
//        String orderId = payload.get("razorpay_order_id");
//        String paymentId = payload.get("razorpay_payment_id");
//        String signature = payload.get("razorpay_signature");
//
//        String keySecret = "f4tai92HJiUjP4snjg50rpko";
//        boolean isValid = verifySignature(orderId, paymentId, signature, keySecret);
//
//        if (isValid) {
//            return ResponseEntity.ok("Payment verified successfully");
//        } else {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
//        }
//    }
//
//    public static boolean verifySignature(String orderId, String paymentId, String razorpaySignature, String secret) {
//        try {
//            String data = orderId + "|" + paymentId;
//            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
//            Mac mac = Mac.getInstance("HmacSHA256");
//            mac.init(secretKeySpec);
//            byte[] hmac = mac.doFinal(data.getBytes());
//            String generatedSignature = Hex.encodeHexString(hmac);
//            return generatedSignature.equals(razorpaySignature);
//        } catch (Exception e) {
//            log.error("Invalid Signature : {} " , e.getMessage());
//            return false;
//        }
//    }
//
//
//
//}
