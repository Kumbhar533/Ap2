package com.veefin.razorpay.controller;

import com.veefin.invoice.dto.PaymentTransactionListResponseDTO;
import com.veefin.razorpay.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * REST Controller for Payment Transaction operations
 */
@RestController
@RequestMapping("/payment-transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class PaymentTransactionController {

    private final PaymentTransactionService paymentTransactionService;

    /**
     * Get paginated list of payment transactions with filtering and searching
     * 
     * @param page - Page number (1-indexed, default: 1)
     * @param size - Page size (default: 20)
     * @param search - Search term (searches in payment ID, order ID, invoice UUID, payment method)
     * @param status - Filter by payment status (SUCCESS, FAILED, PENDING)
     * @param paymentMethod - Filter by payment method (UPI, CARD, etc.)
     * @param invoiceUuid - Filter by invoice UUID
     * @param currency - Filter by currency (INR, USD, etc.)
     * @param minAmount - Minimum amount filter
     * @param maxAmount - Maximum amount filter
     * @return Paginated list of payment transactions
     */
    @GetMapping
    public ResponseEntity<PaymentTransactionListResponseDTO> getPaymentTransactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String invoiceUuid,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount
    ) {
        PaymentTransactionListResponseDTO response = paymentTransactionService.getPaymentTransactionsList(
                page, size, search, status, paymentMethod, invoiceUuid, currency, minAmount, maxAmount
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Download payment receipt as PDF
     *
     * @param identifier - Transaction identifier (UUID, Razorpay Payment ID, or Razorpay Order ID)
     * @return PDF file as downloadable attachment
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadPaymentReceipt(@RequestParam String identifier) {
        try {
            log.info("Downloading payment receipt for identifier: {}", identifier);

            byte[] pdfBytes = paymentTransactionService.downloadPaymentReceipt(identifier);

            // Set headers for PDF download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "payment_receipt_" + identifier + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            log.info("Payment receipt generated successfully for identifier: {}", identifier);
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("Error generating PDF for identifier: {}", identifier, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

