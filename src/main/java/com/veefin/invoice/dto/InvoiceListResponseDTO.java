package com.veefin.invoice.dto;

/**
 * Type alias for Invoice List Response
 * Uses the generic ApiListResponse with InvoiceResponseDTO type
 */
public class InvoiceListResponseDTO extends ApiListResponse<InvoiceResponseDTO> {
    // This class now inherits all functionality from ApiListResponse
    // Specific to InvoiceResponseDTO type
}

