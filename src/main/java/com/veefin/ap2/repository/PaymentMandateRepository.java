package com.veefin.ap2.repository;

import com.veefin.ap2.entity.PaymentMandateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMandateRepository extends JpaRepository<PaymentMandateEntity, Long> {
    PaymentMandateEntity findByPaymentMandateId(String paymentMandateId);
}
