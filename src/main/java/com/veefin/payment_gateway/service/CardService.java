package com.veefin.payment_gateway.service;

import com.veefin.payment_gateway.entity.model.Card;
import com.veefin.payment_gateway.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CardService {


    private final CardRepository cardRepository;
    public Card getCardDetails(String last4){
        Optional<Card> byCardLast4 = cardRepository.findByCardLast4(last4);
        return byCardLast4.orElse(null);
    }

    public Card getCardDetails(String customerId, String last4){
        Optional<Card> byCardLast4 = cardRepository.findByProviderCustomerIdAndCardLast4(customerId, last4);
        return byCardLast4.orElse(null);
    }


    public List<Card> getAllUserCards(String userId){
        return cardRepository.findByUserId(userId);
    }
}
