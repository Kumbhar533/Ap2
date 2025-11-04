package com.veefin.invoice.service;

import com.veefin.invoice.enums.InvoiceStatus;
import com.veefin.invoice.entity.FileMetadata;
import com.veefin.invoice.repository.FileMetaDataRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileMetaDataRepository repository;
    private final OcrService ocrService;

    @Transactional
    public void uploadInvoice(MultipartFile file) {
        FileMetadata savedFile = null;
        File tempFile = null;

        try {
            // 🔹 Create a temporary file
            tempFile = File.createTempFile("invoice_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);
            log.info(" Temporary file created at: {}", tempFile.getAbsolutePath());

            // 🔹 Save metadata (you can store temp file path or original name)
            FileMetadata invoiceFile = FileMetadata.builder()
                    .fileName(file.getOriginalFilename())
                    .filePath(tempFile.getAbsolutePath())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .status(InvoiceStatus.UPLOADED)
                    .build();

            savedFile = repository.save(invoiceFile);

            // 🔹 Process the invoice using OCR
            try {
                ocrService.processInvoice(tempFile);
                savedFile.setStatus(InvoiceStatus.PROCESSED);
                repository.save(savedFile);
                log.info("OCR processing completed for file: {}", file.getOriginalFilename());
            } catch (Exception e) {
                log.error("OCR processing failed for file: {}", file.getOriginalFilename(), e);
                savedFile.setStatus(InvoiceStatus.FAILED);
                repository.save(savedFile);
                throw new RuntimeException("Invoice processing failed: " + e.getMessage());
            }

        } catch (Exception e) {
            log.error("Invoice upload failed for file: {}", file.getOriginalFilename(), e);

            if (savedFile != null) {
                try {
                    savedFile.setStatus(InvoiceStatus.FAILED);
                    repository.save(savedFile);
                } catch (Exception dbEx) {
                    log.error("Failed to update file status to FAILED", dbEx);
                }
            }

            throw new RuntimeException("Invoice upload failed: " + e.getMessage());
        } finally {
            // 🔹 Always delete temporary file after processing
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                    log.info(" Temporary file deleted: {}", tempFile.getName());
                } catch (IOException ex) {
                    log.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath(), ex);
                }
            }
        }
    }
}
