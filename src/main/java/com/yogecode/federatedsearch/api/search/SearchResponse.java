package com.yogecode.federatedsearch.api.search;

import java.util.List;
import java.util.Map;

public record SearchResponse(
        String requestId,
        String rootEntity,
        int page,
        int size,
        long total,
        List<Map<String, Object>> results,
        List<PartialFailureResponse> partialFailures
) {
}
