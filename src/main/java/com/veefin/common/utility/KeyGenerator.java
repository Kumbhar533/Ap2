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
                    -----BEGIN PRIVATE KEY-----
                    MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDTJKyoHOvJV0zw
                    e9NApPrjE0NszHEOYKPx94XRx6VhVHiQgdTjfBSeLr5HGNqmXZp2eTu7Vo7Fe87z
                    iDXZccVQQhh5hPjz2AtjQDxErlg4wqSmCs+9ulYM7seBFiWGoWk2ZfG1vSpWP1jJ
                    XQoTyrtB7pF9i+S/TDybGsKo58z76Y/aNvQGJdu+lDjr06/ZMFRV0a4vSoIoJWln
                    XWTyEOXJfhBovV+c7ZFI8oowKX2B9304BnCoPjvdLcI3paY5BJ6l+WsoUrGIlnN+
                    lvJPF4J7smRW6omjtQwqq8oSeIdKdrl6HcoYxh3SdIMyCR6eXoDGLjnO3RztrPP0
                    ejeRMkKZAgMBAAECggEAMa150A4J7fPT3usLX7TRcLbaENGjMgJL4ITf3+UsxJMm
                    0L0zVRifEz6lNz4iR61TR9erVZ7+dXl1Tjg4j0Ik7ga3EnRWTK0CtOXqqDArAqPN
                    MGHv0dBzKZTOrNEEoEH+6rUeoydwPB2oaqww0EpNN6yFX7gX6GhsBAROU4ZCZOx3
                    vRdi4RM9YWWh/mBxaUZIxPTk5SUyH7AaVQXchXaIHD4KPcN1M/05HO8GKJi7yV5a
                    B/dJRJhQ11T/vY6YILGYetIM50bjoubjp4sfvlFj0pGJgUotNGejUGxCN+5bTCw+
                    A29PTDyK1jvS/4ttfwbI++mprPsTGXwXABckQ7iSWQKBgQD+K8AViEHmDImB8CjD
                    cJnRKUPMCim1KpsEYeQVBsa5mpqpz6TAOal6EIBsxH0KsPOqpxWiWcIB8upuKVoQ
                    8tkN5e6z3hAN+sSE81wLZHHdu6iKdWD9wQMUN4ZWj4HpNWwXymeEFpjOCr+YMlIK
                    BxjUTaRxBau9F1Yjz1u5e68tPQKBgQDUqafnlypjPmw/Q2pxhcnJvQRTuJjW7+gD
                    1EDDinKD3StDq4FATpSm5QiDnUiDN8f9Qxyh2PKPgY36yLevrwTFagqJthAWDjsD
                    9kcGeGdupM39dnUXqM4uPQzrPH5QJFcGM8/DNMI7yUogta2dzURDKKlva93r2ivO
                    DvPeTiU4jQKBgQDOddVi6Dq3Vsm/zcge8XFQsJdMv5ife8tN5QJzffyguZAWjf/C
                    0VP0PkFDmNwFejWEEpMFPKnWEW8SKu7pQ4rCulqKqsAZyvAlGtVSv7Wqqou+ZJhz
                    p3wLyQqLd6jMOcPjE1vAtOZMv/hf9cgkMx2dHrCLylJ1l+Y10nMsmOZz9QKBgGHs
                    5ZYs86XTfsD66C7yLmMpGK78l9SRkEMDH/dq7yRtWZjFhrT3+leHq43/hP53Vy32
                    E18rqTYPsDNE7eCGNSo6lDOplGqzjtVaOIZwNyCp3vjR7UVBw/9iuWow5vE8ap4+
                    iXMX/kDNhcbeId5CQSazuKHIB96tDfLKy90YGueZAoGBAJbfPytIQAr7JV2B50nX
                    LPzl3SaOh5J0K/IcqMCYIE4k86dC6utE4R37p42HBUoYae5UHxdz928UIzAGbBkd
                    fw40X48CWrPn7n4AxX4MiNWB0CGqH2Z5xnobmKMHzIUozp/gE2e56sJMwcQFHmAt
                    XFVazSxca8WaQyCjC7T0g0DF
                    -----END PRIVATE KEY-----
                    """;

    private static final String DEMO_PUBLIC_KEY_PEM = """
        -----BEGIN PUBLIC KEY-----
        MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzR+UZYn5dM6j3gqgh/1y
        AbxgIbKjXk1/MOe3KXmDkw7pR1AKyK+g5xphsVZr1M27yvAQ1y77m1p7yL4wSsx4
        2Z2V6j2+c0qzpDk7UdR4FqfWZzGu7tJxYl/ZwZrDFLqHryv4+S4dIYxG6lMshR6O
        YlS7UQZf5sNqPf3ZDx6bqZB8G7DqQ9VrNZoP3AyvVfV9Fj5jJxCq+Zt6PrHkp9Gv
        2QpVYkDJEZDBaqxY3jztuJzN7v77iE8Ay2Y5o06Xk0l8ZvR8aK+Cl2aYDB4oY+Vq
        VbZVGw1zp8x3tB6FZ1I+O9WZpLEqShk8/Eh6/p4nHh+q9rX7p7XkWQ+R/H8a5Fkx
        YwIDAQAB
        -----END PUBLIC KEY-----
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
