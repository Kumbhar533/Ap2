package com.veefin.invoice.controller;


import com.veefin.common.dto.ResponseDTO;
import com.veefin.invoice.dto.InvoiceListResponseDTO;
import com.veefin.invoice.entity.FileMetadata;
import com.veefin.invoice.service.FileService;
import com.veefin.invoice.service.InvoiceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MerchantController {

    private final FileService invoiceFileService;
    private final InvoiceDataService invoiceDataService;

    @PostMapping(value = "/upload-invoice")
    public ResponseEntity<ResponseDTO> uploadInvoice(@RequestParam("file") MultipartFile file) throws IOException {
            invoiceFileService.uploadInvoice(file);
            return ResponseEntity.ok()
                    .body(new ResponseDTO(HttpStatus.OK.value(), "Invoice uploaded successfully"));

    }

    /**
     * Get paginated list of invoices with filtering, searching, and sorting
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param search Search term (optional) - searches across invoice number, merchant name, file name, uuid
     * @param status Filter by status (optional) - UPLOADED, PROCESSED, FAILED, PAID, PENDING
     * @param merchantName Filter by merchant name (optional)
     * @param invoiceNumber Filter by invoice number (optional)
     * @param minAmount Filter by minimum amount (optional)
     * @param maxAmount Filter by maximum amount (optional)
     * @return Paginated list of invoices sorted by createdAt descending
     */
    @GetMapping("/invoices")
    public ResponseEntity<InvoiceListResponseDTO> getInvoices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String merchantName,
            @RequestParam(required = false) String invoiceNumber,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount
    ) {
        InvoiceListResponseDTO response = invoiceDataService.getInvoicesList(
                page, size, search, status, merchantName, invoiceNumber, minAmount, maxAmount
        );
        return ResponseEntity.ok(response);
    }
}
