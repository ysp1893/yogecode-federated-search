package com.yogecode.federatedsearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private String credentialSecret = "change-me-federated-search-secret";

    public String getCredentialSecret() {
        return credentialSecret;
    }

    public void setCredentialSecret(String credentialSecret) {
        this.credentialSecret = credentialSecret;
    }
}
