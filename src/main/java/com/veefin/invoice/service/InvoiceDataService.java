package com.veefin.invoice.service;

import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

        StringBuilder response = new StringBuilder("**Available Invoices:**\n\n");

        for (InvoiceData invoice : invoices) {
            response.append(String.format(
                    "🧾 **Invoice:** %s\n" +
                            "🏪 **Merchant:** %s\n" +
                            "💰 **Amount:** ₹%.2f\n" +
                            "📅 **Due Date:** %s\n" +
                            "📊 **Status:** %s\n" +
                            "🆔 **ID:** %s\n\n",
                    invoice.getInvoiceNumber(),
                    invoice.getMerchantName(),
                    invoice.getTotalAmount(),
                    invoice.getDueDate(),
                    invoice.getStatus(),
                    invoice.getUuid()
            ));
        }

        response.append("💡 **To pay any invoice, just say:** 'Pay invoice [number]' or 'Process payment for [merchant]'");

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
}
