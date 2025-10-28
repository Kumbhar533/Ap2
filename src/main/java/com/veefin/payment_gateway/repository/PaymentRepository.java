package com.veefin.payment_gateway.repository;

import com.veefin.payment_gateway.entity.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentTransaction, Long>,
                                           JpaSpecificationExecutor<PaymentTransaction> {
    PaymentTransaction findByUuid(String uuid);
    PaymentTransaction findByTransactionId(String transactionId);
}
