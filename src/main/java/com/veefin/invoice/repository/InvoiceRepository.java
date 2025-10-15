package com.veefin.invoice.repository;

import com.veefin.invoice.entity.InvoiceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceData, Long> {

    InvoiceData findByUuid(String uuid);
}