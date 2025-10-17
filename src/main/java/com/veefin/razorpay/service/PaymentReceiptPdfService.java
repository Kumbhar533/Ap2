package com.veefin.razorpay.service;

import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.service.InvoiceDataService;
import com.veefin.razorpay.entity.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Service for generating PDF receipts for payment transactions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReceiptPdfService {

    private final InvoiceDataService invoiceDataService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    /**
     * Generate PDF receipt for a payment transaction
     *
     * @param transaction Payment transaction entity
     * @return PDF as byte array
     * @throws IOException if PDF generation fails
     */
    public byte[] generatePaymentReceipt(PaymentTransaction transaction) throws IOException {
        log.info("Generating PDF receipt for transaction: {}", transaction.getUuid());

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float margin = 50;
                float yPosition = page.getMediaBox().getHeight() - margin;
                float pageWidth = page.getMediaBox().getWidth();

                // Get invoice details if available
                InvoiceData invoice = null;
                if (transaction.getInvoiceUuid() != null) {
                    invoice = invoiceDataService.getInvoiceById(transaction.getInvoiceUuid());
                }

                // Header - Title
                PDFont fontBold = PDType1Font.HELVETICA_BOLD;
                PDFont fontRegular = PDType1Font.HELVETICA;

                contentStream.setFont(fontBold, 24);
                String title = "PAYMENT RECEIPT";
                float titleWidth = fontBold.getStringWidth(title) / 1000 * 24;
                contentStream.beginText();
                contentStream.newLineAtOffset((pageWidth - titleWidth) / 2, yPosition);
                contentStream.showText(title);
                contentStream.endText();
                yPosition -= 40;

                // Horizontal line
                contentStream.setLineWidth(2f);
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(pageWidth - margin, yPosition);
                contentStream.stroke();
                yPosition -= 30;

                // Transaction Details Section
                contentStream.setFont(fontBold, 14);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Transaction Details");
                contentStream.endText();
                yPosition -= 25;

                // Transaction ID
                yPosition = addLabelValuePair(contentStream, margin, yPosition, 
                    "Transaction ID:", transaction.getUuid());

                // Razorpay Order ID
                if (transaction.getRazorpayOrderId() != null) {
                    yPosition = addLabelValuePair(contentStream, margin, yPosition, 
                        "Order ID:", transaction.getRazorpayOrderId());
                }

                // Razorpay Payment ID
                if (transaction.getRazorpayPaymentId() != null) {
                    yPosition = addLabelValuePair(contentStream, margin, yPosition, 
                        "Payment ID:", transaction.getRazorpayPaymentId());
                }

                // Payment Method
                yPosition = addLabelValuePair(contentStream, margin, yPosition, 
                    "Payment Method:", transaction.getPaymentMethod() != null ? transaction.getPaymentMethod() : "N/A");

                // Status
                yPosition = addLabelValuePair(contentStream, margin, yPosition, 
                    "Status:", transaction.getStatus());

                // Amount
                String amountStr = String.format("%s %.2f", 
                    transaction.getCurrency() != null ? transaction.getCurrency() : "INR", 
                    transaction.getAmount());
                yPosition = addLabelValuePair(contentStream, margin, yPosition, 
                    "Amount:", amountStr);

                // Transaction Date
                String dateStr = transaction.getCreatedAt() != null ? 
                    transaction.getCreatedAt().format(DATE_FORMATTER) : "N/A";
                yPosition = addLabelValuePair(contentStream, margin, yPosition, 
                    "Transaction Date:", dateStr);

                yPosition -= 20;

                // Invoice Details Section (if available)
                if (invoice != null) {
                    // Horizontal line
                    contentStream.setLineWidth(1f);
                    contentStream.moveTo(margin, yPosition);
                    contentStream.lineTo(pageWidth - margin, yPosition);
                    contentStream.stroke();
                    yPosition -= 25;

                    contentStream.setFont(fontBold, 14);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText("Invoice Details");
                    contentStream.endText();
                    yPosition -= 25;

                    // Invoice Number
                    yPosition = addLabelValuePair(contentStream, margin, yPosition, 
                        "Invoice Number:", invoice.getInvoiceNumber());

                    // Merchant Name
                    yPosition = addLabelValuePair(contentStream, margin, yPosition, 
                        "Merchant Name:", invoice.getMerchantName());

                    // Invoice Amount
                    String invoiceAmountStr = String.format("INR %.2f", invoice.getTotalAmount());
                    yPosition = addLabelValuePair(contentStream, margin, yPosition, 
                        "Invoice Amount:", invoiceAmountStr);

                    // Due Date
                    yPosition = addLabelValuePair(contentStream, margin, yPosition, 
                        "Due Date:", invoice.getDueDate());

                    // Invoice Status
                    yPosition = addLabelValuePair(contentStream, margin, yPosition, 
                        "Invoice Status:", invoice.getStatus() != null ? invoice.getStatus().name() : "N/A");

                    yPosition -= 20;
                }

                // Footer Section
                contentStream.setLineWidth(1f);
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(pageWidth - margin, yPosition);
                contentStream.stroke();
                yPosition -= 25;

                // Success message or note
                if ("SUCCESS".equalsIgnoreCase(transaction.getStatus())) {
                    contentStream.setFont(fontBold, 12);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText("Payment Successful");
                    contentStream.endText();
                    yPosition -= 20;
                }

                // Thank you message
                contentStream.setFont(fontRegular, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Thank you for your payment!");
                contentStream.endText();
                yPosition -= 30;

                // Footer note
                contentStream.setFont(fontRegular, 8);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("This is a computer-generated receipt and does not require a signature.");
                contentStream.endText();

                // Page number at bottom
                contentStream.setFont(fontRegular, 8);
                String pageNum = "Page 1 of 1";
                float pageNumWidth = fontRegular.getStringWidth(pageNum) / 1000 * 8;
                contentStream.beginText();
                contentStream.newLineAtOffset((pageWidth - pageNumWidth) / 2, 30);
                contentStream.showText(pageNum);
                contentStream.endText();
            }

            document.save(outputStream);
            log.info("PDF receipt generated successfully for transaction: {}", transaction.getUuid());
            return outputStream.toByteArray();
        }
    }

    /**
     * Helper method to add label-value pairs to the PDF
     */
    private float addLabelValuePair(PDPageContentStream contentStream, float x, float y,
                                     String label, String value) throws IOException {
        // Label (bold)
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(label);
        contentStream.endText();

        // Value (regular)
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        contentStream.beginText();
        contentStream.newLineAtOffset(x + 150, y);
        contentStream.showText(value != null ? value : "N/A");
        contentStream.endText();

        return y - 18; // Move to next line
    }
}

