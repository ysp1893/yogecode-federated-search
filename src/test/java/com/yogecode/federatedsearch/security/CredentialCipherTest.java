package com.yogecode.federatedsearch.security;

import com.yogecode.federatedsearch.config.SecurityProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialCipherTest {

    @Test
    void encryptsAndDecryptsRoundTrip() {
        SecurityProperties properties = new SecurityProperties();
        properties.setCredentialSecret("unit-test-secret");

        CredentialCipher cipher = new CredentialCipher(properties);

        String encrypted = cipher.encrypt("p@ssw0rd");

        assertTrue(encrypted.startsWith("{aes}"));
        assertNotEquals("p@ssw0rd", encrypted);
        assertEquals("p@ssw0rd", cipher.decrypt(encrypted));
    }

    @Test
    void keepsLegacyNoopCredentialsReadable() {
        SecurityProperties properties = new SecurityProperties();
        properties.setCredentialSecret("unit-test-secret");

        CredentialCipher cipher = new CredentialCipher(properties);

        assertEquals("{noop}legacy-secret", cipher.decrypt("{noop}legacy-secret"));
    }
}
