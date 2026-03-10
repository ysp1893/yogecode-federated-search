package com.yogecode.federatedsearch.api.search;

public record PartialFailureResponse(
        String entity,
        String source,
        String message
) {
}

