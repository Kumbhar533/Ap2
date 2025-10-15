package com.veefin.invoice.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InvoiceParserService {


        public Map<String, String> parseInvoiceFields(String text) {
            Map<String, String> result = new HashMap<>();

            // Fixed field names with flexible value extraction
            result.put("merchant_name", extractMerchantName(text));
            result.put("invoice_number", extractInvoiceNumber(text));
            result.put("total_amount", extractTotalAmount(text));
            result.put("due_date", extractDueDate(text));

            return result;
        }

        private String extractMerchantName(String text) {
            // Look for company names (usually first meaningful line)
            String[] lines = text.split("\n");
            for (String line : lines) {
                line = line.trim();
                // Skip empty lines, GSTIN, and common headers
                if (line.length() > 3 &&
                        !line.matches(".*GSTIN.*") &&
                        !line.matches(".*Invoice.*") &&
                        !line.matches(".*Field.*") &&
                        !line.matches(".*Value.*") &&
                        line.matches(".*[A-Za-z].*")) {
                    return cleanValue(line);
                }
            }
            return "Unknown";
        }

        private String extractInvoiceNumber(String text) {
            // Look for any alphanumeric pattern that could be invoice number
            Pattern pattern = Pattern.compile("([A-Z0-9\\-/_.]{3,20})", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                String candidate = matcher.group(1);
                // Skip common non-invoice patterns
                if (!candidate.matches(".*(?:GSTIN|Field|Value|pdf).*") &&
                        candidate.matches(".*[A-Z].*[0-9].*|.*[0-9].*[A-Z].*")) {
                    return cleanValue(candidate);
                }
            }
            return "Unknown";
        }

    private String extractTotalAmount(String text) {
        // First, clean OCR artifacts from the text
        String cleanText = text.replaceAll("[■●▪♦◆▲►]", ""); // Remove OCR symbols

        // Look for amount patterns near "Total Amount" keywords
        Pattern contextPattern = Pattern.compile("Total\\s*Amount[^0-9]*([0-9,]+\\.?[0-9]{0,2})", Pattern.CASE_INSENSITIVE);
        Matcher contextMatcher = contextPattern.matcher(cleanText);

        if (contextMatcher.find()) {
            return cleanValue(contextMatcher.group(1));
        }

        // Fallback: Look for any number with decimal places (likely amounts)
        Pattern pattern = Pattern.compile("([0-9,]+\\.[0-9]{2})"); // Prefer amounts with 2 decimal places
        Matcher matcher = pattern.matcher(cleanText);

        String largestAmount = "Unknown";
        double maxValue = 0;

        while (matcher.find()) {
            String candidate = matcher.group(1).replace(",", "");
            try {
                double value = Double.parseDouble(candidate);
                // Assume largest number is the total amount
                if (value > maxValue && value > 10) { // Minimum threshold
                    maxValue = value;
                    largestAmount = matcher.group(1); // Keep original format with commas
                }
            } catch (NumberFormatException e) {
                // Skip invalid numbers
            }
        }

        return largestAmount;
    }



        private String extractDueDate(String text) {
            // Look for date patterns
            Pattern[] datePatterns = {
                    Pattern.compile("([0-9]{1,2}[-/][A-Za-z]{3}[-/][0-9]{4})"),
                    Pattern.compile("([0-9]{1,2}[-/][0-9]{1,2}[-/][0-9]{4})"),
                    Pattern.compile("([0-9]{4}[-/][0-9]{1,2}[-/][0-9]{1,2})")
            };

            for (Pattern pattern : datePatterns) {
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    return cleanValue(matcher.group(1));
                }
            }

            return "Unknown";
        }

    private String cleanValue(String value) {
        if (value == null) return "Unknown";

        // Remove OCR artifacts and clean up
        value = value.replaceAll("[■●▪♦◆▲►]", ""); // Remove OCR symbols
        value = value.replaceAll("[^A-Za-z0-9\\s\\-/_.,:.]", ""); // Keep only valid chars
        value = value.trim();

        return value.isEmpty() ? "Unknown" : value;
    }





//    public Map<String, String> parseInvoiceFields(String text) {
//        Map<String, String> result = new HashMap<>();
//
//        // Updated regex patterns for your invoice format
//        result.put("invoice_number", findMatch("Invoice\\s*#?\\s*:?\\s*([A-Z0-9-]+)", text));
//        result.put("total_amount", findMatch("(?:Total\\s*Amount|Amount\\s*Due)\\s*:?\\s*([0-9,.]+)", text));
//        result.put("due_date", findMatch("Due\\s*Date\\s*:?\\s*([0-9]{1,2}-[A-Za-z]{3}-[0-9]{4})", text));
//        result.put("merchant_name", findMatch("^([A-Za-z\\s]+(?:Pvt\\s*Ltd|Ltd|Inc|Corp))", text));
//
//        return result;
//    }
//
//    private String findMatch(String regex, String text) {
//        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
//        Matcher matcher = pattern.matcher(text);
//        if (matcher.find()) {
//            return matcher.group(1).trim();
//        }
//        return "Unknown";
//    }


}
