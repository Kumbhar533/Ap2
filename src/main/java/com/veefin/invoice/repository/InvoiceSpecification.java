package com.veefin.invoice.repository;

import com.veefin.invoice.entity.InvoiceData;
import com.veefin.invoice.enums.InvoiceStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class InvoiceSpecification {

    public static Specification<InvoiceData> filterInvoices(
            String search,
            String status,
            String merchantName,
            String invoiceNumber,
            Double minAmount,
            Double maxAmount
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search across multiple fields
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate searchPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("invoiceNumber")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("merchantName")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fileName")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("uuid")), searchPattern)
                );
                predicates.add(searchPredicate);
            }

            // Filter by status
            if (status != null && !status.trim().isEmpty()) {
                try {
                    InvoiceStatus invoiceStatus = InvoiceStatus.valueOf(status.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), invoiceStatus));
                } catch (IllegalArgumentException e) {
                    // Invalid status, ignore this filter
                }
            }

            // Filter by merchant name
            if (merchantName != null && !merchantName.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("merchantName")),
                        "%" + merchantName.toLowerCase() + "%"
                ));
            }

            // Filter by invoice number
            if (invoiceNumber != null && !invoiceNumber.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("invoiceNumber")),
                        "%" + invoiceNumber.toLowerCase() + "%"
                ));
            }

            // Filter by amount range
            if (minAmount != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("totalAmount"), minAmount));
            }

            if (maxAmount != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("totalAmount"), maxAmount));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}

