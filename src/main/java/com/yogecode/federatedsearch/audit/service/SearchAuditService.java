package com.yogecode.federatedsearch.audit.service;

import com.yogecode.federatedsearch.api.search.SearchRequest;
import com.yogecode.federatedsearch.api.search.SearchResponse;

public interface SearchAuditService {

    void record(SearchRequest request, SearchResponse response, long executionTimeMs);
}

