package com.yogecode.federatedsearch.audit.service;

import com.yogecode.federatedsearch.api.search.SearchRequest;
import com.yogecode.federatedsearch.api.search.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingSearchAuditService implements SearchAuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSearchAuditService.class);

    @Override
    public void record(SearchRequest request, SearchResponse response, long executionTimeMs) {
        LOGGER.info(
                "search requestId={} rootEntity={} results={} executionTimeMs={}",
                response.requestId(),
                response.rootEntity(),
                response.total(),
                executionTimeMs
        );
    }
}

