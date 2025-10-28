package com.veefin.ap2.repository;

import com.veefin.ap2.entity.UserPublicKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPublicKeyRepository extends JpaRepository<UserPublicKey, Long> {

    Optional<UserPublicKey> findByUserId(String userId);

    Optional<UserPublicKey> findByUserIdAndIsActive(String userId, Boolean isActive);

    boolean existsByUserId(String userId);
}