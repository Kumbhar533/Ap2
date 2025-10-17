package com.veefin.razorpay.service;

import com.veefin.common.exception.ResourceNotFoundException;
import com.veefin.invoice.dto.ApiListResponse;
import com.veefin.invoice.dto.PaginationDTO;
import com.veefin.invoice.dto.PaymentTransactionListResponseDTO;
import com.veefin.invoice.dto.PaymentTransactionResponseDTO;
import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.service.InvoiceDataService;
import com.veefin.razorpay.entity.PaymentTransaction;
import com.veefin.razorpay.repository.PaymentRepository;
import com.veefin.razorpay.specification.PaymentTransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final InvoiceDataService invoiceDataService;
    private final DemoTransactionData demoTransactionData;
    private final PaymentReceiptPdfService paymentReceiptPdfService;

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String getAllPaymentHistory() {
        List<PaymentTransaction> payments = paymentRepository.findAll();

        if (payments.isEmpty()) {
            return "📋 No payment history found.";
        }

        StringBuilder response = new StringBuilder();
        response.append("==================================================\n");
        response.append("                  PAYMENT HISTORY                  \n");
        response.append("==================================================\n\n");

        for (PaymentTransaction payment : payments) {
            InvoiceData invoice = invoiceDataService.getInvoiceById(payment.getInvoiceUuid());

            String formattedDate = payment.getCreatedAt() != null
                    ? payment.getCreatedAt().format(dateFormatter)
                    : "Unknown";

            String invoiceNumber = invoice != null ? invoice.getInvoiceNumber() : "Unknown";
            String merchantName = invoice != null ? invoice.getMerchantName() : "Unknown";

            // Construct local download link
            String downloadLink = String.format(
                    "[Download Receipt for %s](http://localhost:8080/payment-transactions/download?identifier=%s)",
                    invoiceNumber,
                    payment.getUuid()
            );

            response.append(String.format("""
                            Payment ID   : %s
                            Invoice      : %s
                            Merchant     : %s
                            Amount       : ₹%.2f
                            Method       : %s
                            Status       : %s
                            Date         : %s
                            Download     : %s
                            --------------------------------------------------
                            
                            """,
                    payment.getRazorpayPaymentId(),
                    invoiceNumber,
                    merchantName,
                    payment.getAmount(),
                    payment.getPaymentMethod(),
                    payment.getStatus(),
                    formattedDate,
                    downloadLink
            ));
        }

        response.append("To view more details, type: \"show payment history\"\n");
        response.append("==================================================\n");

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

        response.append(String.format("""
==================================================
           PAYMENT HISTORY FOR INVOICE %s
==================================================

""", invoice.getInvoiceNumber()));

        for (PaymentTransaction payment : payments) {

            String downloadLink = String.format(
                    "[Download Receipt for %s](http://localhost:8080/payment-transactions/download?identifier=%s)",
                    invoice.getInvoiceNumber(),
                    payment.getUuid()
            );
            response.append(String.format("""
Payment ID   : %s
Merchant     : %s
Amount       : ₹%.2f
Method       : %s
Status       : %s
Date         : %s
Order ID     : %s
Download link: %s
--------------------------------------------------

""",
                    payment.getRazorpayPaymentId(),
                    invoice.getMerchantName(),
                    payment.getAmount(),
                    payment.getPaymentMethod(),
                    payment.getStatus(),
                    payment.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")),
                    payment.getRazorpayOrderId(),
                    downloadLink
            ));
        }

        response.append("To view more details, type: \"show payment history\"");

        return response.toString();

    }

    /**
     * Get paginated and filtered list of payment transactions
     */
    public PaymentTransactionListResponseDTO getPaymentTransactionsList(
            int page,
            int size,
            String search,
            String status,
            String paymentMethod,
            String invoiceUuid,
            String currency,
            Double minAmount,
            Double maxAmount
    ) {
        // Create specification for filtering
        Specification<PaymentTransaction> spec = PaymentTransactionSpecification.filterPaymentTransactions(
                search, status, paymentMethod, invoiceUuid, currency, minAmount, maxAmount
        );

        // Create pageable with sorting by createdAt descending
        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // Fetch data
        Page<PaymentTransaction> paymentPage = paymentRepository.findAll(spec, pageable);

        List<PaymentTransactionResponseDTO> data = new ArrayList<>(demoTransactionData.getDemoTransactions());

        // Convert to DTOs
        List<PaymentTransactionResponseDTO> content = paymentPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        data.addAll(content);

        // Build pagination info
        PaginationDTO pagination = PaginationDTO.builder()
                .currentPage(paymentPage.getNumber() + 1)
                .totalPages(paymentPage.getTotalPages())
                .totalElements(paymentPage.getTotalElements())
                .pageSize(paymentPage.getSize())
                .first(paymentPage.isFirst())
                .last(paymentPage.isLast())
                .empty(paymentPage.isEmpty())
                .build();

        // Build response
        PaymentTransactionListResponseDTO response = new PaymentTransactionListResponseDTO();
        response.setSuccess(true);
        response.setData(ApiListResponse.DataWrapper.<PaymentTransactionResponseDTO>builder()
                .content(data)
                .pagination(pagination)
                .build());
        response.setMessage("Payment transactions retrieved successfully");
        return response;
    }

    /**
     * Convert PaymentTransaction entity to DTO
     */
    private PaymentTransactionResponseDTO convertToDTO(PaymentTransaction payment) {
        // Fetch invoice details if available
        InvoiceData invoice = null;
        if (payment.getInvoiceUuid() != null && !payment.getInvoiceUuid().isEmpty()) {
            invoice = invoiceDataService.getInvoiceById(payment.getInvoiceUuid());
        }

        String formattedDate = payment.getCreatedAt() != null
                ? payment.getCreatedAt().format(dateFormatter)
                : "Unknown";

        return PaymentTransactionResponseDTO.builder()
                .transactionId(payment.getRazorpayOrderId())
                .reference(payment.getRazorpayPaymentId())
                .amount(payment.getAmount())
                .vendorName(invoice != null ? invoice.getMerchantName() : null)
                .date(formattedDate)
                .type("invoice")
                .description("Payment received - partial invoice set")
                .reconciled(false)
                .build();
    }

    /**
     * Find payment transaction by identifier (UUID, Razorpay Payment ID, or Razorpay Order ID)
     * and generate PDF receipt
     *
     * @param identifier - Transaction UUID, Razorpay Payment ID, or Razorpay Order ID
     * @return PDF receipt as byte array
     * @throws IOException if PDF generation fails
     * @throws ResourceNotFoundException if transaction not found
     */
    public byte[] downloadPaymentReceipt(String identifier) throws IOException {
        PaymentTransaction transaction = findTransactionByIdentifier(identifier);

        if (transaction == null) {
            throw new ResourceNotFoundException(
                "Payment transaction not found with identifier: " + identifier
            );
        }

        return paymentReceiptPdfService.generatePaymentReceipt(transaction);
    }

    /**
     * Find payment transaction by various identifiers
     *
     * @param identifier - Can be UUID, Razorpay Payment ID, or Razorpay Order ID
     * @return PaymentTransaction or null if not found
     */
    private PaymentTransaction findTransactionByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }
        PaymentTransaction transaction = paymentRepository.findByUuid(identifier);

        return transaction;
    }

}
