package com.veefin.payment_gateway.service;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.braintreegateway.TransactionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veefin.ap2.dto.PaymentMandate;
import com.veefin.ap2.dto.PaymentMandateContents;
import com.veefin.ap2.entity.PaymentMandateEntity;
import com.veefin.ap2.service.CryptographicService;
import com.veefin.ap2.service.PaymentMandateService;
import com.veefin.common.exception.BraintreeException;
import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.enums.InvoiceStatus;
import com.veefin.invoice.repository.InvoiceRepository;
import com.veefin.invoice.service.InvoiceVectorService;
import com.veefin.payment_gateway.entity.dto.TransactionResponseDto;
import com.veefin.payment_gateway.entity.model.Card;
import com.veefin.payment_gateway.entity.model.PaymentTransaction;
import com.veefin.payment_gateway.enums.PaymentEnums;
import com.veefin.payment_gateway.repository.CardRepository;
import com.veefin.payment_gateway.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrainTreeService {

    private final BraintreeGateway gateway;
    private final PaymentMandateService paymentMandateService;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionVectorStore transactionVectorStore;
    private final InvoiceVectorService invoiceVectorService;
    private final CryptographicService cryptoService;
    private final CardRepository cardRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();



    public TransactionResponseDto createTransaction(Double amount, String token) {
        try {
            TransactionRequest request = new TransactionRequest()
                    .amount(BigDecimal.valueOf(amount))
                    .paymentMethodToken(token)
                    .orderId("ORD-" + UUID.randomUUID())
                    .options()
                    .submitForSettlement(true)
                    .done();

            Result<Transaction> result = gateway.transaction().sale(request);

            if (result.isSuccess()) {
                TransactionResponseDto transactionResponseDto = processPayment(result);
                //token,status
                log.info("Transaction created successfully. ID: {}", transactionResponseDto.getTransactionId());
                return transactionResponseDto;
            }else {
                log.error("Transaction failed: {}", result.getMessage());
                    throw new BraintreeException("Transaction failed: " + result.getMessage());
            }
        } catch (Exception e) {
            log.error("Exception during Braintree transaction: {}", e.getMessage());
            throw new BraintreeException("Braintree transaction error", e);
        }
    }




    public void processPaymentSuccess(String paymentMandateId, String invoiceUuid,TransactionResponseDto transactionResponseDto) throws Exception {

        Optional<Card> card = cardRepository.findByProviderCustomerIdAndCardLast4(transactionResponseDto.getCustomerId(), transactionResponseDto.getLast4());
        if (card.isEmpty()) {
            throw new SecurityException("Card not found for token: " + transactionResponseDto.getCustomerId());
        }

        //  Load PaymentMandate by ID
        PaymentMandateEntity mandateEntity = paymentMandateService.getPaymentMandateById(paymentMandateId);
        if (mandateEntity == null) {
            throw new SecurityException("Payment mandate not found: " + paymentMandateId);
        }

        //  Verify backend signature on payment mandate
        PaymentMandate mandate = convertToDto(mandateEntity);
        boolean isValid = cryptoService.verifyAgentSignature(
                objectMapper.writeValueAsString(mandate.getPaymentMandateContents()),
                mandate.getBackendSignature()
        );
        if (!isValid) {
            throw new SecurityException("Invalid AP2 payment mandate signature");
        }


        //  Store payment transaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .invoiceUuid(invoiceUuid)
                .transactionId(transactionResponseDto.getTransactionId())
                .amount(transactionResponseDto.getAmount()) // Convert from paise to rupees
                .currency(transactionResponseDto.getCurrencyCode())
                .paymentMethod(transactionResponseDto.getPaymentMethod())
                .fromAccount(card.get().getCardLast4())
                .toAccount("merchant@payment-gateway")
                .fromAccountType("CARD")
                .toAccountType("MERCHANT_ACCOUNT")
                .status(transactionResponseDto.getStatus())
                .build();

        PaymentTransaction savedTransaction = paymentRepository.save(transaction);

        // Store in vector DB
        transactionVectorStore.storePaymentInVectorDB(savedTransaction);

        //  Update invoice status
        InvoiceData invoice = invoiceRepository.findByUuid(invoiceUuid);
        if (invoice != null) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);
            log.info("Invoice {} marked as PAID", invoiceUuid);

            // Update invoice in vector DB
            invoiceVectorService.updateInvoiceInVectorDB(invoice);
        }

        //  Update payment mandate status
        mandateEntity.setStatus("PROCESSED");
        mandateEntity.setGatewayPaymentId(transactionResponseDto.getTransactionId());
        paymentMandateService.updatePaymentMandateEntity(mandateEntity);
    }

    private PaymentMandate convertToDto(PaymentMandateEntity entity) {
        PaymentMandateContents contents = new PaymentMandateContents();
        contents.setPaymentMandateId(entity.getPaymentMandateId());
        contents.setCartId(entity.getCartId());
        contents.setCartHash(entity.getCartHash());
        contents.setTotalAmount(entity.getAmount());
        contents.setCurrency(entity.getCurrency());
        contents.setMerchantAgent(entity.getMerchantName());
        contents.setPaymentMethod(entity.getPaymentMethod());
        contents.setTimestamp(entity.getTimestamp());

        PaymentMandate mandate = new PaymentMandate();
        mandate.setPaymentMandateContents(contents);
        mandate.setBackendSignature(entity.getBackendSignature());
        mandate.setStatus(entity.getStatus());

        return mandate;
    }


    public TransactionResponseDto processPayment(Result<Transaction> result) throws Exception {

        Transaction transaction = result.getTarget();
        return TransactionResponseDto.builder()
                .transactionId(transaction.getId())
                .currencyCode("INR")
                .amount(transaction.getAmount().doubleValue())
                .paymentMethod(transaction.getPaymentInstrumentType())
                .customerId(transaction.getCustomer().getId())
                .orderId(transaction.getOrderId())
                .status(transaction.getStatus().name())
                .last4(transaction.getCreditCard().getLast4())
                .build();

    }

}
