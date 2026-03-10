package com.yogecode.federatedsearch.connector.spi;

import com.yogecode.federatedsearch.api.search.SearchFilterRequest;
import com.yogecode.federatedsearch.metadata.model.EntityMetadataRecord;

import java.util.List;

public record QueryExecutionRequest(
        EntityMetadataRecord entity,
        List<SearchFilterRequest> filters,
        int page,
        int size
) {
}

