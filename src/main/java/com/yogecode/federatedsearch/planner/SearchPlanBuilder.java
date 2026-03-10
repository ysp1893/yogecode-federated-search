package com.yogecode.federatedsearch.planner;

import com.yogecode.federatedsearch.api.search.SearchRequest;
import com.yogecode.federatedsearch.cache.model.SearchMetadataContext;
import com.yogecode.federatedsearch.cache.service.SearchMetadataContextService;
import com.yogecode.federatedsearch.metadata.service.MetadataService;
import org.springframework.stereotype.Component;

@Component
public class SearchPlanBuilder {

    private final MetadataService metadataService;
    private final SearchMetadataContextService searchMetadataContextService;

    public SearchPlanBuilder(
            MetadataService metadataService,
            SearchMetadataContextService searchMetadataContextService
    ) {
        this.metadataService = metadataService;
        this.searchMetadataContextService = searchMetadataContextService;
    }

    public SearchPlan build(SearchRequest request) {
        String rootEntityCode = resolveRootEntity(request);
        SearchMetadataContext context = searchMetadataContextService.getContext(rootEntityCode);
        return new SearchPlan(
                rootEntityCode,
                context.rootEntity(),
                context.includedRelations(request.include()),
                request
        );
    }

    private String resolveRootEntity(SearchRequest request) {
        if (request.entity() != null && !request.entity().isBlank()) {
            return request.entity();
        }
        return metadataService.resolveEntityByKeyword(request.keyword())
                .orElseThrow(() -> new IllegalArgumentException("Either entity or a registered keyword is required"));
    }
}
