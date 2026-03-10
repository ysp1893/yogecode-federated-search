package com.yogecode.federatedsearch.security;

import org.springframework.stereotype.Component;

@Component
public class CredentialCipher {

    public String encrypt(String rawValue) {
        return "{noop}" + rawValue;
    }

    public String decrypt(String encryptedValue) {
        return encryptedValue;
    }
}
