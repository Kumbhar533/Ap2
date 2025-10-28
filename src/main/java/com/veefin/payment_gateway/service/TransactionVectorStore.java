package com.veefin.payment_gateway.service;

import com.veefin.payment_gateway.entity.model.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionVectorStore {

    private final VectorStore vectorStore;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void storePaymentInVectorDB(PaymentTransaction payment) {
        try {
            // Create searchable content for the payment
            String content = buildPaymentContent(payment);

            // Create metadata for filtering and search
            Map<String, Object> metadata = Map.of(
                    "paymentId", payment.getTransactionId(),
                    "fromAccount", payment.getFromAccount(),
                    "toAccount", payment.getToAccount(),
                    "invoiceUuid", payment.getInvoiceUuid(),
                    "amount", payment.getAmount(),
                    "currency", payment.getCurrency(),
                    "paymentMethod", payment.getPaymentMethod(),
                    "status", payment.getStatus(),
                    "documentType", "PAYMENT",
                    "createdAt", payment.getCreatedAt() != null ?
                            payment.getCreatedAt().format(DATE_FORMATTER) : "Unknown"
            );

            // Create document for vector storage
            Document document = new Document(
                     payment.getUuid(),
                    content,
                    metadata
            );

            // Store in vector database
            vectorStore.add(List.of(document));

            log.info("Payment transaction stored in vector DB: {}", payment.getTransactionId());

        } catch (Exception e) {
            log.error("Failed to store payment in vector DB: {}", e.getMessage());
        }
    }

    private String buildPaymentContent(PaymentTransaction payment) {
        return String.format("""
                Payment Transaction Details:
                Payment ID: %s
                Invoice UUID: %s
                From Account: %s
                To Account: %s
                Amount: %.2f %s
                Payment Method: %s
                Status: %s
                Created: %s
                """,
                payment.getTransactionId(),
                payment.getInvoiceUuid(),
                payment.getFromAccount(),
                payment.getToAccount(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getCreatedAt() != null ?
                        payment.getCreatedAt().format(DATE_FORMATTER) : "Unknown"
        );
    }

    public List<Document> searchPayments(String query, int topK) {
        return vectorStore.similaritySearch(query);
    }

    public List<Document> searchPaymentsByStatus(String status, int topK) {
        String query = "payment status " + status;
        return vectorStore.similaritySearch(query);
    }

    public List<Document> searchPaymentsByAmount(double amount, int topK) {
        String query = "payment amount " + amount;
        return vectorStore.similaritySearch(query);
    }
}