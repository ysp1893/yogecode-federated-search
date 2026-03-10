package com.yogecode.federatedsearch.planner;

import com.yogecode.federatedsearch.api.search.SearchRequest;
import com.yogecode.federatedsearch.metadata.model.EntityMetadataRecord;
import com.yogecode.federatedsearch.metadata.model.RelationMetadataRecord;

import java.util.List;

public record SearchPlan(
        String rootEntityCode,
        EntityMetadataRecord rootEntity,
        List<RelationMetadataRecord> includedRelations,
        SearchRequest request
) {
}

