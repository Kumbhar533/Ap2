package com.veefin.common.utility;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
public class KeyGenerator {


    /**
     * 🔧 Demo RSA Private Key in PKCS#8 PEM format (for testing only)
     */
    private static final String DEMO_PRIVATE_KEY_PEM =
            """
                   """;

    private static final String DEMO_PUBLIC_KEY_PEM = """
       
        """;

    public String simulateUserSignature(String intentJSON) {
        try {
            // Parse PEM private key
            PrivateKey privateKey = parsePrivateKeyFromPEM();

            // Sign the intent JSON
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(intentJSON.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = signature.sign();
            String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

            log.info("User signature created for intent JSON (length: {})", signatureBase64.length());
            return signatureBase64;

        } catch (Exception e) {
            log.error("Failed to create user signature: {}", e.getMessage());
            throw new RuntimeException("User signature creation failed", e);
        }
    }

    /**
     * 🔧 Compute SHA-256 hash
     */
    public String computeHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            String hashBase64 = Base64.getEncoder().encodeToString(hashBytes);

            log.debug("SHA-256 hash computed (length: {})", hashBase64.length());
            return hashBase64;

        } catch (Exception e) {
            log.error("Failed to compute hash: {}", e.getMessage());
            throw new RuntimeException("Hash computation failed", e);
        }
    }

    /**
     * 🔧 Parse RSA private key from PEM format
     */
    private PrivateKey parsePrivateKeyFromPEM() throws Exception {
        try {
            // Remove PEM headers and all whitespace/newlines
            String privateKeyPEM = KeyGenerator.DEMO_PRIVATE_KEY_PEM
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "")  // Remove all whitespace including newlines
                    .trim();

            // Validate Base64 content
            if (privateKeyPEM.isEmpty()) {
                throw new IllegalArgumentException("Empty private key content after PEM processing");
            }

            // Decode Base64
            byte[] keyBytes;
            try {
                keyBytes = Base64.getDecoder().decode(privateKeyPEM);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid Base64 encoding in private key", e);
            }

            // Create private key
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            PrivateKey privateKey = keyFactory.generatePrivate(spec);

            // Validate key by checking algorithm
            if (!"RSA".equals(privateKey.getAlgorithm())) {
                throw new IllegalArgumentException("Expected RSA private key, got: " + privateKey.getAlgorithm());
            }

            log.debug("Successfully parsed RSA private key");
            return privateKey;

        } catch (Exception e) {
            log.error("Failed to parse private key from PEM: {}", e.getMessage());
            throw new Exception("Private key parsing failed: " + e.getMessage(), e);
        }
    }
}
