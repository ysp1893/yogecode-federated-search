package com.yogecode.federatedsearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    private boolean preloadOnStartup = true;
    private String keyPrefix = "federated-search";
    private final Redis redis = new Redis();

    public boolean isPreloadOnStartup() {
        return preloadOnStartup;
    }

    public void setPreloadOnStartup(boolean preloadOnStartup) {
        this.preloadOnStartup = preloadOnStartup;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public Redis getRedis() {
        return redis;
    }

    public static class Redis {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
