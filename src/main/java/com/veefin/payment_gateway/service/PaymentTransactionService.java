package com.veefin.payment_gateway.service;

import com.veefin.common.exception.ResourceNotFoundException;
import com.veefin.invoice.dto.ApiListResponse;
import com.veefin.invoice.dto.PaginationDTO;
import com.veefin.invoice.dto.PaymentTransactionListResponseDTO;
import com.veefin.invoice.dto.PaymentTransactionResponseDTO;
import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.service.InvoiceDataService;
import com.veefin.payment_gateway.entity.model.PaymentTransaction;
import com.veefin.payment_gateway.enums.PaymentEnums;
import com.veefin.payment_gateway.repository.PaymentRepository;
import com.veefin.payment_gateway.specification.PaymentTransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final InvoiceDataService invoiceDataService;
    private final DemoTransactionData demoTransactionData;
    private final PaymentReceiptPdfService paymentReceiptPdfService;
    private final TransactionVectorStore transactionVectorStore;
    private final ChatClient chatClient;

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
                .toList();
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
                .transactionId(payment.getTransactionId())
                .reference("REF-"+ UUID.randomUUID())
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

        return paymentRepository.findByUuid(identifier);
    }



    public String handlePaymentQuery(String userPrompt) {
        try {
            // RAG: Search vector DB, limit to top 5 relevant payments
            List<Document> relevantPayments = transactionVectorStore.searchPayments(userPrompt, 5);

            if (relevantPayments.isEmpty()) {
                return "No payment transactions found matching your query.";
            }

            boolean hasInvalidData = relevantPayments.stream()
                    .anyMatch(doc -> {
                        Object invoiceUuid = doc.getMetadata().get("invoiceUuid");
                        return invoiceUuid == null || invoiceUuid.toString().trim().isEmpty() ||
                                invoiceUuid.toString().equals("null");
                    });

            StringBuilder context = new StringBuilder();
            if (hasInvalidData) {
                userPrompt = "just say no transactions found in few words";
            }else {

                // Build minimal context (only essential fields)

                for (Document doc : relevantPayments.subList(0, Math.min(5, relevantPayments.size()))) {
                    context.append("Payment ID: ").append(doc.getMetadata().get("paymentId"))
                            .append(", Invoice UUID: ").append(doc.getMetadata().get("invoiceUuid"))
                            .append(", Amount: ₹").append(doc.getMetadata().get("amount"))
                            .append(", Status: ").append(doc.getMetadata().get("status"))
                            .append(", Date: ").append(doc.getMetadata().get("createdAt"))
                            .append("\n");
                }
            }

            // Shortened prompt, still provides same info for LLM
            String ragPrompt = String.format("""
            You are a payment transaction assistant. Use the data below to answer the user's query naturally.

            User Query: %s
            Payment Data: %s
            Respond helpfully:
        """, userPrompt, context.toString());

            // Call LLM
            return chatClient.prompt()
                    .user(ragPrompt)
                    .call()
                    .content();

        } catch (Exception e) {
            return "Failed to process payment query: " + e.getMessage();
        }
    }


    public void updatePaymentTransaction(String transactionId) {
        PaymentTransaction paymentTransaction = paymentRepository.findByTransactionId(transactionId);
        paymentTransaction.setStatus(PaymentEnums.SUCCESS.name());
        paymentRepository.save(paymentTransaction);
    }


}
