package com.veefin.razorpay.repository;

import com.veefin.razorpay.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentTransaction, Long> {
    PaymentTransaction findByRazorpayPaymentId(String razorpayPaymentId);
    List<PaymentTransaction> findByInvoiceUuid(String invoiceUuid);
}
