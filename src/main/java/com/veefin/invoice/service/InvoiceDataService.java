package com.veefin.invoice.service;

import com.veefin.invoice.dto.ApiListResponse;
import com.veefin.invoice.dto.InvoiceListResponseDTO;
import com.veefin.invoice.dto.InvoiceResponseDTO;
import com.veefin.invoice.dto.PaginationDTO;
import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.repository.InvoiceRepository;
import com.veefin.invoice.repository.InvoiceSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class InvoiceDataService {

    private final InvoiceRepository invoiceRepository;

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

}

