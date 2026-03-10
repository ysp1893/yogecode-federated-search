package com.yogecode.federatedsearch.search.service;

import com.yogecode.federatedsearch.api.search.PartialFailureResponse;
import com.yogecode.federatedsearch.api.search.SearchRequest;
import com.yogecode.federatedsearch.api.search.SearchResponse;
import com.yogecode.federatedsearch.audit.service.SearchAuditService;
import com.yogecode.federatedsearch.config.SearchProperties;
import com.yogecode.federatedsearch.connector.spi.ConnectorRegistry;
import com.yogecode.federatedsearch.connector.spi.QueryExecutionRequest;
import com.yogecode.federatedsearch.connector.spi.QueryExecutionResult;
import com.yogecode.federatedsearch.datasource.model.RegisteredDataSource;
import com.yogecode.federatedsearch.datasource.service.DataSourceService;
import com.yogecode.federatedsearch.planner.SearchPlan;
import com.yogecode.federatedsearch.planner.SearchPlanBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DefaultFederatedSearchService implements FederatedSearchService {

    private final SearchPlanBuilder searchPlanBuilder;
    private final DataSourceService dataSourceService;
    private final ConnectorRegistry connectorRegistry;
    private final SearchProperties searchProperties;
    private final SearchAuditService searchAuditService;

    public DefaultFederatedSearchService(
            SearchPlanBuilder searchPlanBuilder,
            DataSourceService dataSourceService,
            ConnectorRegistry connectorRegistry,
            SearchProperties searchProperties,
            SearchAuditService searchAuditService
    ) {
        this.searchPlanBuilder = searchPlanBuilder;
        this.dataSourceService = dataSourceService;
        this.connectorRegistry = connectorRegistry;
        this.searchProperties = searchProperties;
        this.searchAuditService = searchAuditService;
    }

    @Override
    public SearchResponse search(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        SearchPlan plan = searchPlanBuilder.build(request);

        RegisteredDataSource rootSource = dataSourceService.findById(plan.rootEntity().sourceId())
                .orElseThrow(() -> new IllegalArgumentException("Datasource not found for root entity"));

        int page = request.page() == null ? 0 : Math.max(request.page(), 0);
        int size = normalizeSize(request.size());

        QueryExecutionResult rootResult = connectorRegistry.get(rootSource.dbType())
                .execute(new QueryExecutionRequest(plan.rootEntity(), request.filters(), request.fields(), page, size));

        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> row : rootResult.rows()) {
            Map<String, Object> assembled = new LinkedHashMap<>();
            assembled.put(plan.rootEntityCode(), row);
            results.add(assembled);
        }

        SearchResponse response = new SearchResponse(
                UUID.randomUUID().toString(),
                plan.rootEntityCode(),
                page,
                size,
                results.size(),
                results,
                List.<PartialFailureResponse>of()
        );
        searchAuditService.record(request, response, System.currentTimeMillis() - startTime);
        return response;
    }

    private int normalizeSize(Integer requestedSize) {
        if (requestedSize == null) {
            return searchProperties.getDefaultPageSize();
        }
        return Math.min(Math.max(requestedSize, 1), searchProperties.getMaxPageSize());
    }
}

