package com.veefin.ap2.repository;

import com.veefin.ap2.entity.CartMandate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartMandateRepository extends JpaRepository<CartMandate, Long> {

    CartMandate findTopByInvoiceUuidAndStatusOrderByCreatedAtDesc( String uuid,String status);

}
