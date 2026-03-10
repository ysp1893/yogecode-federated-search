package com.yogecode.federatedsearch.cache.model;

import java.time.Instant;

public record CacheRefreshResponse(
        Instant timestamp,
        String scope,
        String key,
        String status,
        String message,
        boolean redisEnabled
) {
}
