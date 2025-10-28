package com.veefin.ap2.repository;

import com.veefin.ap2.entity.IntentMandateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntentMandateRepository extends JpaRepository<IntentMandateEntity, Long> {

    IntentMandateEntity findByIntentHash(String intentHash);
    IntentMandateEntity findByInvoiceUuid(String invoiceUuid);
}