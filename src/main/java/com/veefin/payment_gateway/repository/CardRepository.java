package com.veefin.payment_gateway.repository;

import com.veefin.payment_gateway.entity.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByProviderCustomerIdAndCardLast4(String customerId, String last4);
    Optional<Card> findByCardLast4(String last4);

    List<Card> findByUserId(String userId);
}
