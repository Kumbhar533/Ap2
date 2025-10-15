package com.veefin.invoice.service;

import com.veefin.invoice.enums.InvoiceStatus;
import com.veefin.invoice.entity.FileMetadata;
import com.veefin.invoice.repository.FileMetaDataRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
public class FileService {


    private final FileMetaDataRepository repository;
    private final OcrService ocrService;

    private final String UPLOAD_DIR = "private-uploads/invoices/";

    @Transactional
    public void uploadInvoice(MultipartFile file) throws IOException {
        // Ensure directory exists
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));

            // Save file to local directory
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path targetPath = Paths.get(UPLOAD_DIR + fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Save metadata in DB
            FileMetadata invoiceFile = FileMetadata.builder()
                    .fileName(fileName)
                    .filePath(targetPath.toAbsolutePath().toString())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .status(InvoiceStatus.UPLOADED)

                    .build();

            FileMetadata savedFile = repository.save(invoiceFile);

            // Process the invoice with OCR
            try {
                File pdfFile = new File(targetPath.toAbsolutePath().toString());
                ocrService.processInvoice(pdfFile);

                // Update status to PROCESSED
                savedFile.setStatus(InvoiceStatus.PROCESSED);
                repository.save(savedFile);
            } catch (Exception e) {
                e.printStackTrace();
                // Update status to FAILED if OCR processing fails
                savedFile.setStatus(InvoiceStatus.FAILED);
                repository.save(savedFile);
                throw new RuntimeException("Invoice processing failed: " + e.getMessage());
            }
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Invoice upload failed: " + e.getMessage());
        }

    }
    }



