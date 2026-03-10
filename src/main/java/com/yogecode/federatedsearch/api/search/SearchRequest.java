package com.yogecode.federatedsearch.api.search;

import jakarta.validation.Valid;

import java.util.List;

public record SearchRequest(
        String entity,
        String keyword,
        @Valid List<SearchFilterRequest> filters,
        List<String> include,
        Integer page,
        Integer size
) {
}

