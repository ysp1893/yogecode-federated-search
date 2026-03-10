package com.yogecode.federatedsearch.planner;

import com.yogecode.federatedsearch.api.search.SearchRequest;
import com.yogecode.federatedsearch.metadata.model.EntityMetadataRecord;
import com.yogecode.federatedsearch.metadata.model.RelationMetadataRecord;
import com.yogecode.federatedsearch.metadata.service.MetadataService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchPlanBuilder {

    private final MetadataService metadataService;

    public SearchPlanBuilder(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    public SearchPlan build(SearchRequest request) {
        String rootEntityCode = resolveRootEntity(request);
        EntityMetadataRecord rootEntity = metadataService.findEntityByCode(rootEntityCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown root entity: " + rootEntityCode));

        List<RelationMetadataRecord> relations = metadataService.findRelationsFrom(rootEntityCode).stream()
                .filter(relation -> request.include() != null && request.include().contains(relation.toEntityCode()))
                .toList();

        return new SearchPlan(rootEntityCode, rootEntity, relations, request);
    }

    private String resolveRootEntity(SearchRequest request) {
        if (request.entity() != null && !request.entity().isBlank()) {
            return request.entity();
        }
        return metadataService.resolveEntityByKeyword(request.keyword())
                .orElseThrow(() -> new IllegalArgumentException("Either entity or a registered keyword is required"));
    }
}

