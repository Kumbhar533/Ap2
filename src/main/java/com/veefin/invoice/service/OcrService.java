package com.veefin.invoice.service;

import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.enums.InvoiceStatus;
import com.veefin.invoice.repository.InvoiceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {


    private final InvoiceRepository invoiceRepository;
    private final InvoiceParserService invoiceParserService;
    private final InvoiceVectorService invoiceVectorService;

    @Value("${tesseract.datapath:C:/Program Files/Tesseract-OCR/tessdata}")
    private String tesseractPath;

    @Value("${ghostscript.path:}")
    private String ghostscriptPath;

    public void processInvoice(File pdfFile) throws IOException {
        String extractedText = extractText(pdfFile);

        // Step 2: Parse key invoice fields (via regex/AI)
        Map<String, String> fieldMap = invoiceParserService.parseInvoiceFields(extractedText);

        String totalAmountStr = fieldMap.getOrDefault("total_amount", "0.0");
        totalAmountStr = totalAmountStr.replaceAll(",", ""); // Remove commas
        // Step 3: Build & Save entity
        InvoiceData invoiceData = InvoiceData.builder()
                .fileName(pdfFile.getName())
                .merchantName(fieldMap.getOrDefault("merchant_name", "Unknown"))
                .invoiceNumber(fieldMap.getOrDefault("invoice_number", "N/A"))
                .totalAmount(Double.valueOf(totalAmountStr))
                .dueDate(fieldMap.getOrDefault("due_date", "N/A"))
                .status(InvoiceStatus.PENDING)
                .rawText(extractedText)
                .fieldMap(fieldMap)
                .build();


        InvoiceData save = invoiceRepository.save(invoiceData);
        // store in Vector DB
        invoiceVectorService.storeInvoiceInVectorDB(save);
    }

    private String extractText(File file) {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (!text.trim().isEmpty()) {
                // fallback to OCR
                return doOcr(file);
            }
            return text;
        } catch (Exception e) {
            // fallback if PDFBox fails
            return doOcr(file);
        }
    }

    @PostConstruct
    public void init() {
        // Set Ghostscript path if configured
        if (!ghostscriptPath.isEmpty()) {
            System.setProperty("jna.library.path",
                    ghostscriptPath.substring(0, ghostscriptPath.lastIndexOf("/")));
        }
    }


    private String doOcr(File file) {
        try {
            Tesseract tesseract = new Tesseract();
            if (!tesseractPath.isEmpty()) {
                tesseract.setDatapath(tesseractPath);
            }
            tesseract.setLanguage("eng");
            return tesseract.doOCR(file);
        } catch (TesseractException e) {
            throw new RuntimeException("OCR failed: " + e.getMessage());
        }
    }
}
