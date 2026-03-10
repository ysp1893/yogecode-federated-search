package com.yogecode.federatedsearch.api.search;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

public record SearchRequest(
        String entity,
        String keyword,
        @Valid List<SearchFilterRequest> filters,
        List<String> include,
        List<String> fields,
        Map<String, List<String>> entityFields,
        String sortBy,
        String sortDirection,
        Integer page,
        Integer size
) {
}
