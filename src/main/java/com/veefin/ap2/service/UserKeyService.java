package com.veefin.ap2.service;

import com.veefin.ap2.entity.UserPublicKey;
import com.veefin.ap2.repository.UserPublicKeyRepository;
import com.veefin.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserKeyService {

    /**
     * Register new user public key with validation
     */

    public final UserPublicKeyRepository userPublicKeyRepo;
    private final CryptographicService cryptographicService;


    public void registerUserPublicKey(String userId, String publicKeyStr) {
        try {
            // 1. Validate the public key format and strength
            validatePublicKey(publicKeyStr);

            // 2. Check if user already has an active key
            Optional<UserPublicKey> existingKey = userPublicKeyRepo.findByUserIdAndIsActive(userId, true);
            if (existingKey.isPresent()) {
                throw new RuntimeException("User already has an active public key. Use rotateUserKey() instead.");
            }

            // 3. Store the new key
            cryptographicService.addUserPublicKey(userId, publicKeyStr);

            log.info("User {} public key registered successfully", userId);
        } catch (Exception e) {
            log.error("Failed to register public key for user {}: {}", userId, e.getMessage());
            throw new ValidationException("Public key registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validate public key format and strength
     */
    private void validatePublicKey(String publicKeyStr) throws Exception {
        if (publicKeyStr == null || publicKeyStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Public key cannot be null or empty");
        }

        try {
            PublicKey key = cryptographicService.loadPublicKeyFromString(publicKeyStr.trim());

            // Check algorithm
            if (!"RSA".equals(key.getAlgorithm())) {
                throw new IllegalArgumentException("Only RSA keys are supported, got: " + key.getAlgorithm());
            }

            // Check key size (RSA keys should be at least 2048 bits)
            if (key instanceof java.security.interfaces.RSAPublicKey rsaKey) {
                int keySize = rsaKey.getModulus().bitLength();
                if (keySize < 2048) {
                    throw new IllegalArgumentException("RSA key size must be at least 2048 bits, got: " + keySize);
                }
            }

            log.debug("Public key validation passed for algorithm: {}", key.getAlgorithm());

        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid public key format: " + e.getMessage(), e);
        }
    }
}
