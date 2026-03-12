package com.yogecode.federatedsearch.security;

import com.yogecode.federatedsearch.config.SecurityProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CredentialCipher {

    private static final Logger log = LoggerFactory.getLogger(CredentialCipher.class);
    private static final String NOOP_PREFIX = "{noop}";
    private static final String AES_PREFIX = "{aes}";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecurityProperties securityProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialCipher(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @PostConstruct
    void warnOnDefaultSecret() {
        if ("change-me-federated-search-secret".equals(securityProperties.getCredentialSecret())) {
            log.warn("Using default credential secret. Set app.security.credential-secret for stronger protection.");
        }
    }

    public String encrypt(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(rawValue.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];

            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);

            return AES_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encrypt datasource credential", exception);
        }
    }

    public String decrypt(String encryptedValue) {
        if (encryptedValue == null) {
            return null;
        }
        if (encryptedValue.startsWith(NOOP_PREFIX)) {
            return encryptedValue;
        }
        if (!encryptedValue.startsWith(AES_PREFIX)) {
            return encryptedValue;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedValue.substring(AES_PREFIX.length()));
            if (payload.length <= IV_LENGTH) {
                throw new IllegalArgumentException("Encrypted credential payload is invalid");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[payload.length - IV_LENGTH];

            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to decrypt datasource credential", exception);
        }
    }

    private SecretKeySpec secretKey() throws Exception {
        String secret = securityProperties.getCredentialSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Credential secret must not be blank");
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }
}
