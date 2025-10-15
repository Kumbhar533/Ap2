package com.veefin.invoice.controller;


import com.veefin.common.dto.ResponseDTO;
import com.veefin.invoice.entity.FileMetadata;
import com.veefin.invoice.service.FileService;
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
    @PostMapping(value = "/upload-invoice")
    public ResponseEntity<String> uploadInvoice(@RequestParam("file") MultipartFile file) throws IOException {
            invoiceFileService.uploadInvoice(file);
            return ResponseEntity.ok()
                    .body("File uploaded successfully");

    }
}
