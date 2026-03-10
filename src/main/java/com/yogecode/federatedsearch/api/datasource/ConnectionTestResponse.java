package com.yogecode.federatedsearch.api.datasource;

public record ConnectionTestResponse(
        Long sourceId,
        String status,
        String message
) {
}

