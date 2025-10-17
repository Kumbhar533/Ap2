package com.veefin.razorpay.service;

import com.veefin.invoice.dto.PaymentTransactionResponseDTO;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class DemoTransactionData {

    public List<PaymentTransactionResponseDTO> getDemoTransactions() {
        return Arrays.asList(
                PaymentTransactionResponseDTO.builder()
                        .transactionId("TXN-2024-001")
                        .date("2024-04-01")
                        .vendorName("Global Tech Solutions")
                        .amount(500000.0)
                        .type("income")
                        .description("Client payment for Q1 services")
                        .reference("REF-001-2024")
                        .reconciled(false)
                        .build(),

                PaymentTransactionResponseDTO.builder()
                        .transactionId("TXN-2024-002")
                        .date("2024-04-10")
                        .vendorName("Tax Genie Pvt Ltd")
                        .amount(150000.0)
                        .type("expense")
                        .description("Vendor payment for Tax Genie Pvt Ltd")
                        .reference("REF-002-2024")
                        .reconciled(false)
                        .build(),

                PaymentTransactionResponseDTO.builder()
                        .transactionId("TXN-2024-003")
                        .date("2024-05-02")
                        .vendorName("Bright Office Supplies")
                        .amount(45000.0)
                        .type("expense")
                        .description("Office materials and supplies")
                        .reference("REF-003-2024")
                        .reconciled(false)
                        .build(),

                PaymentTransactionResponseDTO.builder()
                        .transactionId("TXN-2024-004")
                        .date("2024-05-15")
                        .vendorName("Enterprise Solutions Ltd")
                        .amount(350000.0)
                        .type("income")
                        .description("Project milestone payment")
                        .reference("REF-004-2024")
                        .reconciled(false)
                        .build(),

                PaymentTransactionResponseDTO.builder()
                        .transactionId("TXN-2024-005")
                        .date("2024-06-15")
                        .vendorName("Creative Prints Ltd")
                        .amount(80000.0)
                        .type("expense")
                        .description("Payment for marketing material")
                        .reference("REF-005-2024")
                        .reconciled(false)
                        .build(),

                PaymentTransactionResponseDTO.builder()
                        .transactionId("TXN-2024-006")
                        .date("2024-07-01")
                        .vendorName("Premium Clients Inc")
                        .amount(275000.0)
                        .type("income")
                        .description("Monthly retainer payment")
                        .reference("REF-006-2024")
                        .reconciled(false)
                        .build(),

                PaymentTransactionResponseDTO.builder()
                        .transactionId("TXN-2024-007")
                        .date("2024-07-12")
                        .vendorName("ABC Consulting LLP")
                        .amount(200000.0)
                        .type("expense")
                        .description("Consulting services payment (multiple invoices)")
                        .reference("REF-007-2024")
                        .reconciled(false)
                        .build(),

                PaymentTransactionResponseDTO.builder()
                        .transactionId("TXN-2024-008")
                        .date("2024-08-05")
                        .vendorName("Zenith Logistics")
                        .amount(120000.0)
                        .type("expense")
                        .description("Logistics and warehouse management fee")
                        .reference("REF-008-2024")
                        .reconciled(false)
                        .build(),

                PaymentTransactionResponseDTO.builder()
                        .transactionId("TXN-2024-009")
                        .date("2024-09-10")
                        .vendorName("Inspire Technologies")
                        .amount(95000.0)
                        .type("income")
                        .description("Client payment for August billing")
                        .reference("REF-009-2024")
                        .reconciled(false)
                        .build(),

                PaymentTransactionResponseDTO.builder()
                        .transactionId("TXN-2024-010")
                        .date("2024-10-01")
                        .vendorName("Delta Manufacturing Co")
                        .amount(175000.0)
                        .type("income")
                        .description("Payment received - partial invoice set")
                        .reference("REF-010-2024")
                        .reconciled(false)
                        .build(),

                PaymentTransactionResponseDTO.builder()
                        .transactionId("TXN-2024-011")
                        .date("2024-10-15")
                        .vendorName("Split Test Vendor")
                        .amount(20000.0)
                        .type("expense")
                        .description("Partial payment for Split Test Vendor")
                        .reference("REF-011-2024")
                        .reconciled(false)
                        .build()
        );
    }
}
