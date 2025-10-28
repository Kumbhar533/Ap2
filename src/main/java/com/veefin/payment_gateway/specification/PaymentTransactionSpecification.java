package com.veefin.payment_gateway.specification;

import com.veefin.payment_gateway.entity.model.PaymentTransaction;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification for filtering Payment Transactions
 */
public class PaymentTransactionSpecification {

    /**
     * Creates a specification for filtering payment transactions
     * 
     * @param search - Search term (searches in razorpayPaymentId, razorpayOrderId, invoiceUuid)
     * @param status - Filter by payment status (SUCCESS, FAILED, PENDING)
     * @param paymentMethod - Filter by payment method (UPI, CARD, etc.)
     * @param invoiceUuid - Filter by invoice UUID
     * @param currency - Filter by currency
     * @param minAmount - Minimum amount filter
     * @param maxAmount - Maximum amount filter
     * @return Specification for filtering
     */
    public static Specification<PaymentTransaction> filterPaymentTransactions(
            String search,
            String status,
            String paymentMethod,
            String invoiceUuid,
            String currency,
            Double minAmount,
            Double maxAmount
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search across multiple fields
            if (search != null && !search.isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate searchPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("razorpayPaymentId")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("razorpayOrderId")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("invoiceUuid")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("paymentMethod")), searchPattern)
                );
                predicates.add(searchPredicate);
            }

            // Filter by status
            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // Filter by payment method
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("paymentMethod"), paymentMethod));
            }

            // Filter by invoice UUID
            if (invoiceUuid != null && !invoiceUuid.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("invoiceUuid"), invoiceUuid));
            }

            // Filter by currency
            if (currency != null && !currency.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("currency"), currency));
            }

            // Filter by minimum amount
            if (minAmount != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }

            // Filter by maximum amount
            if (maxAmount != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}

