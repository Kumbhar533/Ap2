package com.veefin.invoice.service;

import com.veefin.invoice.entity.InvoiceData;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvoiceVectorService {

    private final VectorStore vectorStore;

    public void storeInvoiceInVectorDB(InvoiceData invoice) {
        // Create Document with content and metadata
        Map<String, Object> metadata = Map.of(
                "uuid", invoice.getUuid(),
                "invoiceNumber", invoice.getInvoiceNumber(),
                "totalAmount", invoice.getTotalAmount(),
                "merchantName", invoice.getMerchantName(),
                "status", invoice.getStatus().name(),
                "dueDate", invoice.getDueDate(),
                "createdAt", invoice.getCreatedAt().toString(),
                "updatedAt", invoice.getUpdatedAt().toString(),
                "documentType", "INVOICE"
        );

        Document document = new Document(
                 invoice.getUuid(),
                invoice.getRawText(),
                metadata
        );

        // Add document to vector store (embedding happens automatically)
        vectorStore.add(List.of(document));
    }

    public List<Document> searchSimilarInvoices(String query) {
        return vectorStore.similaritySearch(query);
    }

    /**
     * Update invoice in vector DB when status or other fields change
     * This re-adds the document with the same UUID, which updates it in the vector store
     */
    public void updateInvoiceInVectorDB(InvoiceData invoice) {
        // Reuse the same logic as storeInvoiceInVectorDB
        // Since we use the same UUID, it will update the existing document
        storeInvoiceInVectorDB(invoice);
    }


}