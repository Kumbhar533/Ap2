package com.veefin.payment_gateway.repository;

import com.veefin.payment_gateway.entity.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByProviderCustomerId(String customerId);
}
