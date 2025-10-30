package com.veefin.invoice.service;

import com.veefin.invoice.dto.ApiListResponse;
import com.veefin.invoice.dto.InvoiceListResponseDTO;
import com.veefin.invoice.dto.InvoiceResponseDTO;
import com.veefin.invoice.dto.PaginationDTO;
import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.repository.InvoiceRepository;
import com.veefin.invoice.repository.InvoiceSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class InvoiceDataService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceVectorService invoiceVectorService;
    private final ChatClient chatClient;

    public InvoiceData getInvoiceById(String uuid) {
        return invoiceRepository.findByUuid(uuid);
    }



    public String getAllInvoicesResponse() {
        List<InvoiceData> invoices = invoiceRepository.findAll();

        if (invoices.isEmpty()) {
            return " No invoices found in the system.";
        }

        StringBuilder response = new StringBuilder();

        response.append("""
                           📋**Invoice Summary**

|  Invoice No |         Merchant        |  Amount  |  Status  |  Due Date  |
|-------------|-------------------------|----------|----------|------------|
""");

        for (InvoiceData invoice : invoices) {
            response.append(String.format(
                    "| %s | %s | ₹%.2f | %s | %s |\n\n",
                    invoice.getInvoiceNumber(),
                    invoice.getMerchantName(),
                    invoice.getTotalAmount(),
                    invoice.getStatus(),
                    invoice.getDueDate()
            ));
        }

        response.append("💬 **To pay any invoice, just say:** `Pay invoice [number]` or `Process payment for [merchant]`");

        return response.toString();
    }


    public InvoiceData findInvoiceByIdentifier(String identifier) {
        List<InvoiceData> invoices = invoiceRepository.findAll();

        return invoices.stream()
                .filter(inv ->
                        inv.getInvoiceNumber().equalsIgnoreCase(identifier) ||
                                inv.getMerchantName().toLowerCase().contains(identifier.toLowerCase()) ||
                                inv.getUuid().equals(identifier)
                )
                .findFirst()
                .orElse(null);
    }

    public InvoiceListResponseDTO getInvoicesList(
            int page,
            int size,
            String search,
            String status,
            String merchantName,
            String invoiceNumber,
            Double minAmount,
            Double maxAmount
    ) {
        // Create specification for filtering
        Specification<InvoiceData> spec = InvoiceSpecification.filterInvoices(
                search, status, merchantName, invoiceNumber, minAmount, maxAmount
        );

        // Create pageable with sorting by createdAt descending
        Pageable pageable = PageRequest.of(page-1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Fetch data
        Page<InvoiceData> invoicePage = invoiceRepository.findAll(spec, pageable);

        // Convert to DTOs
        List<InvoiceResponseDTO> content = invoicePage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // Build pagination info
        PaginationDTO pagination = PaginationDTO.builder()
                .currentPage(invoicePage.getNumber() + 1) // Convert to 1-indexed
                .totalPages(invoicePage.getTotalPages())
                .totalElements(invoicePage.getTotalElements())
                .pageSize(invoicePage.getSize())
                .first(invoicePage.isFirst())
                .last(invoicePage.isLast())
                .empty(invoicePage.isEmpty())
                .build();

        // Build final response using helper method
        InvoiceListResponseDTO response = new InvoiceListResponseDTO();
        response.setSuccess(true);
        response.setData(ApiListResponse.DataWrapper.<InvoiceResponseDTO>builder()
                .content(content)
                .pagination(pagination)
                .build());
        response.setMessage("Invoices retrieved successfully");

        return response;
    }

    /**
     * Convert InvoiceData entity to InvoiceResponseDTO
     */
    private InvoiceResponseDTO convertToDTO(InvoiceData invoice) {
        return InvoiceResponseDTO.builder()
                .vendorName(invoice.getMerchantName())
                .docId(invoice.getInvoiceNumber())
                .amount(invoice.getTotalAmount())
                .date(invoice.getDueDate())
                .status(invoice.getStatus() != null ? invoice.getStatus().name() : null)
                .type("invoice")
                .build();
    }


    public List<InvoiceData> getAllInvoiceByMerchantName(String merchantName){
        return invoiceRepository.findByMerchantNameContainingIgnoreCase(merchantName);
    }

    public String handleInvoiceQuery(String userPrompt) {
        try {
            // RAG: Always search vector DB first for relevant context
            List<Document> relevantInvoices = invoiceVectorService.searchSimilarInvoices(userPrompt+", Only return INVOICE documentType data ");

            if (relevantInvoices.isEmpty()) {
                return "No invoices found matching your query.";
            }

            // RAG: Build rich context from retrieved documents
            StringBuilder context = new StringBuilder();
            for (Document doc : relevantInvoices.subList(0, Math.min(5, relevantInvoices.size()))) {
                Object invoiceNumber = doc.getMetadata().get("invoiceNumber");
                Object amount = doc.getMetadata().get("totalAmount");
                Object dueDate = doc.getMetadata().get("dueDate");
                Object status = doc.getMetadata().get("status");
                //Object updatedAt = doc.getMetadata().get("updatedAt");

                // Skip invalid or incomplete invoices
                if (invoiceNumber == null || amount == null) {
                    continue;
                }

                context.append("Invoice: ").append(invoiceNumber)
                        .append(", amount: ₹").append(amount)
                        .append(", Due Date: ").append(dueDate)
                        .append(", Status: ").append(status)
                       // .append(", Updated: ").append(updatedAt)
                        .append("\n");
            }

//            String ragPrompt = String.format("""
//You are an invoice assistant. Use the data below to answer the user's query naturally.
//
//User Query: %s
//Invoice Data: %s
//Respond helpfully:
//""", userPrompt, context.toString());

//            String ragPrompt = String.format("""
//                     You are an invoice assistant. Answer the user based ONLY on the invoice data provided.
//                     User Query: %s
//                     Invoice Data: %s
//                     """,
//                    userPrompt, context.toString());
            String ragPrompt = String.format("""
User message (any language): %s

Understand the message and reply ONLY using the data below.
If it’s about invoices, show them clearly like:

🧾 Invoice: [Invoice ID]
   • Amount: ₹[amount]
   • Due Date: [date]
   • Status: [status]
Data:
%s
""", userPrompt, context.toString());

            log.info("RAG prompt: {}", ragPrompt);
            long startTime = System.currentTimeMillis();

//            String response = chatClient.prompt()
//                    .user(ragPrompt)
//                    .call()
//                    .content();


            String streamedContent = String.join("", Objects.requireNonNull(chatClient.prompt()
                    .user(ragPrompt)
                    .stream()
                    .content()
                    .collectList()
                    .block()));

            System.err.println("RAG response: " + streamedContent);

// Calculate and log time taken
            long endTime = System.currentTimeMillis();
            double timeTakenSeconds = (endTime - startTime) / 1000.0;
            log.info("ChatClient response time: {} seconds", timeTakenSeconds);

            return streamedContent;

        } catch (Exception e) {
            return "Failed to process invoice query: " + e.getMessage();
        }
    }


}

