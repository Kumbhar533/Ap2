package com.veefin.ap2.service;

import com.veefin.ap2.entity.UserPublicKey;
import com.veefin.ap2.repository.UserPublicKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class CryptographicService {

    private final KeyPair agentKeyPair;
    private final Map<String, PublicKey> userPublicKeysCache;

    @Autowired
    private UserPublicKeyRepository userPublicKeyRepo;

    public CryptographicService() {
        this.agentKeyPair = generateAgentKeyPair();
        this.userPublicKeysCache = new HashMap<>();
    }

    /**
     * Generate RSA key pair for backend agent
     */
    private KeyPair generateAgentKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();
            log.info("Agent RSA key pair generated successfully");
            return keyPair;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate agent key pair", e);
        }
    }

    /**
     * Load user public key from database with caching
     */
    private PublicKey getUserPublicKey(String userId) {
        try {
            // Check cache first
            PublicKey cachedKey = userPublicKeysCache.get(userId);
            if (cachedKey != null) {
                return cachedKey;
            }

            // Load from DB
            Optional<UserPublicKey> userKeyEntity = userPublicKeyRepo.findByUserIdAndIsActive(userId, true);
            if (userKeyEntity.isEmpty()) {
                log.warn("No active public key found for user: {}", userId);
                return null;
            }

            PublicKey publicKey = loadPublicKeyFromString(userKeyEntity.get().getPublicKey());

            // Cache it for next time
            userPublicKeysCache.put(userId, publicKey);

            return publicKey;
        } catch (Exception e) {
            log.error("Failed to load public key for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Compute SHA-256 hash
     */
    public String computeSHA256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute SHA-256", e);
        }
    }

    /**
     * Verify user's signature on Intent Mandate
     */
    public boolean verifyUserSignature(String intentJSON, String userSignature) {
        return verifyUserSignature(intentJSON, userSignature, "demo-user");
    }

    public boolean verifyUserSignature(String intentJSON, String userSignature, String userId) {
        try {
            PublicKey userPublicKey = getUserPublicKey(userId);

            if (userPublicKey == null) {
                log.warn("Public key not found for user: {}", userId);
                return false;
            }

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(userPublicKey);
            sig.update(intentJSON.getBytes(StandardCharsets.UTF_8));
            boolean isValid = sig.verify(Base64.getDecoder().decode(userSignature));

            log.info("User signature verification for {}: {}", userId, isValid);
            return isValid;
        } catch (Exception e) {
            log.error("User signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Backend signs data with agent private key
     */
    public String signWithAgentKey(String data) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(agentKeyPair.getPrivate());
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(sig.sign());

            log.debug("Data signed with agent key");
            return signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign with agent key", e);
        }
    }

    /**
     * Verify backend's signature
     */
    public boolean verifyAgentSignature(String data, String signature) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(agentKeyPair.getPublic());
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            boolean isValid = sig.verify(Base64.getDecoder().decode(signature));

            log.debug("Agent signature verification: {}", isValid);
            return isValid;
        } catch (Exception e) {
            log.error("Agent signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sign with merchant key (for merchant ERP)
     */
    public String signWithMerchantKey(String data) {
        // For demo, use agent key. In production, use separate merchant keys
        return signWithAgentKey(data);
    }

    /**
     * Verify merchant signature
     */
    public boolean verifyMerchantSignature(String data, String signature) {
        // For demo, use agent verification. In production, use merchant public key
        return verifyAgentSignature(data, signature);
    }

    /**
     * Sign with processor key (for payment processor)
     */
    public String signWithProcessorKey(String data) {
        // For demo, use agent key. In production, use separate processor keys
        return signWithAgentKey(data);
    }

    /**
     * Verify agent request signature (for inter-agent communication)
     */
    public boolean verifyAgentRequestSignature(String data, String signature, String agentId) {
        // For demo, use agent verification. In production, maintain agent public keys
        return verifyAgentSignature(data, signature);
    }

    /**
     * Get agent public key as string (for sharing with other agents)
     */
    public String getAgentPublicKeyString() {
        try {
            byte[] publicKeyBytes = agentKeyPair.getPublic().getEncoded();
            return Base64.getEncoder().encodeToString(publicKeyBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get agent public key", e);
        }
    }

    /**
     * Get user public key (for demo purposes)
     */
    public PublicKey getUserPublicKey() {
        return getUserPublicKey("demo-user");
    }

    /**
     * Get user public key string
     */
    public String getUserPublicKeyString(String userId) {
        try {
            PublicKey publicKey = getUserPublicKey(userId);
            if (publicKey == null) return null;

            byte[] publicKeyBytes = publicKey.getEncoded();
            return Base64.getEncoder().encodeToString(publicKeyBytes);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Load public key from Base64 string
     */
    PublicKey loadPublicKeyFromString(String publicKeyStr) throws Exception {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyStr);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    /**
     * Load private key from Base64 string
     */
    private PrivateKey loadPrivateKeyFromString(String privateKeyStr) throws Exception {
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyStr);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }

    /**
     * Add user public key to database and cache
     */
    public void addUserPublicKey(String userId, String publicKeyStr) throws Exception {

            // Validate the key first
            PublicKey publicKey = loadPublicKeyFromString(publicKeyStr);

            // Save to database
            UserPublicKey userKeyEntity = new UserPublicKey();
            userKeyEntity.setUserId(userId);
            userKeyEntity.setPublicKey(publicKeyStr);
            userKeyEntity.setKeyAlgorithm("RSA");
            userKeyEntity.setKeySize(2048);
            userKeyEntity.setIsActive(true);

            userPublicKeyRepo.save(userKeyEntity);

            // Update cache
            userPublicKeysCache.put(userId, publicKey);

            log.info("Added public key for user: {}", userId);

    }

    /**
     * Generate key pair for testing
     */
    public KeyPair generateTestKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test key pair", e);
        }
    }

    /**
     * Sign data with test private key
     */
    public String signWithTestKey(String data, PrivateKey privateKey) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign with test key", e);
        }
    }

    /**
     * Encrypt data with public key
     */
    public String encrypt(String data, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypt data with private key
     */
    public String decrypt(String encryptedData, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedData = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }



}