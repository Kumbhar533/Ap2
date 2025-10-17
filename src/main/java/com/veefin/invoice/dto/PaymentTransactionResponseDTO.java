package com.veefin.invoice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for Payment Transaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionResponseDTO {

    private String transactionId;
    private String reference;
    private String vendorName;
    private Double amount;
    private String type;
    private String description;
    private boolean reconciled;
    private String date;

}

