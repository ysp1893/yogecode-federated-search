package com.yogecode.federatedsearch.search.service;

import com.yogecode.federatedsearch.api.search.PartialFailureResponse;
import com.yogecode.federatedsearch.api.search.SearchFilterRequest;
import com.yogecode.federatedsearch.api.search.SearchRequest;
import com.yogecode.federatedsearch.api.search.SearchResponse;
import com.yogecode.federatedsearch.audit.service.SearchAuditService;
import com.yogecode.federatedsearch.cache.model.SearchMetadataContext;
import com.yogecode.federatedsearch.cache.service.SearchMetadataContextService;
import com.yogecode.federatedsearch.common.enums.FilterOperator;
import com.yogecode.federatedsearch.common.enums.RelationType;
import com.yogecode.federatedsearch.config.SearchProperties;
import com.yogecode.federatedsearch.connector.spi.ConnectorRegistry;
import com.yogecode.federatedsearch.connector.spi.QueryExecutionRequest;
import com.yogecode.federatedsearch.connector.spi.QueryExecutionResult;
import com.yogecode.federatedsearch.datasource.model.RegisteredDataSource;
import com.yogecode.federatedsearch.metadata.model.EntityMetadataRecord;
import com.yogecode.federatedsearch.metadata.model.RelationMetadataRecord;
import com.yogecode.federatedsearch.planner.SearchPlan;
import com.yogecode.federatedsearch.planner.SearchPlanBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DefaultFederatedSearchService implements FederatedSearchService {

    private final SearchPlanBuilder searchPlanBuilder;
    private final SearchMetadataContextService searchMetadataContextService;
    private final ConnectorRegistry connectorRegistry;
    private final SearchProperties searchProperties;
    private final SearchAuditService searchAuditService;

    public DefaultFederatedSearchService(
            SearchPlanBuilder searchPlanBuilder,
            SearchMetadataContextService searchMetadataContextService,
            ConnectorRegistry connectorRegistry,
            SearchProperties searchProperties,
            SearchAuditService searchAuditService
    ) {
        this.searchPlanBuilder = searchPlanBuilder;
        this.searchMetadataContextService = searchMetadataContextService;
        this.connectorRegistry = connectorRegistry;
        this.searchProperties = searchProperties;
        this.searchAuditService = searchAuditService;
    }

    @Override
    public SearchResponse search(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        SearchPlan plan = searchPlanBuilder.build(request);
        SearchMetadataContext context = searchMetadataContextService.getContext(plan.rootEntityCode());

        RegisteredDataSource rootSource = context.rootSource();
        int page = request.page() == null ? 0 : Math.max(request.page(), 0);
        int size = normalizeSize(request.size());
        List<String> requestedRootFields = resolveRequestedFields(request, plan.rootEntityCode(), true);
        List<String> rootExecutionFields = ensureFieldPresent(requestedRootFields, relationSourceFields(plan.includedRelations()));

        QueryExecutionResult rootResult = connectorRegistry.get(rootSource.dbType())
                .execute(new QueryExecutionRequest(plan.rootEntity(), request.filters(), rootExecutionFields, page, size));

        Map<String, RelatedEntityBundle> includedData = loadIncludedRelations(context, plan, request, rootResult.rows());

        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> row : rootResult.rows()) {
            Map<String, Object> assembled = new LinkedHashMap<>();
            assembled.put(plan.rootEntityCode(), selectFields(row, requestedRootFields));
            attachIncludedRelations(assembled, row, plan.includedRelations(), includedData);
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

    private Map<String, RelatedEntityBundle> loadIncludedRelations(
            SearchMetadataContext context,
            SearchPlan plan,
            SearchRequest request,
            List<Map<String, Object>> rootRows
    ) {
        if (plan.includedRelations().isEmpty() || rootRows.isEmpty()) {
            return Map.of();
        }

        Map<String, RelatedEntityBundle> bundles = new HashMap<>();
        for (RelationMetadataRecord relation : plan.includedRelations()) {
            EntityMetadataRecord targetEntity = context.entity(relation.toEntityCode());
            if (targetEntity == null) {
                throw new IllegalArgumentException("Unknown related entity: " + relation.toEntityCode());
            }
            Set<Object> joinValues = collectJoinValues(rootRows, relation.fromField());
            List<String> requestedRelatedFields = resolveRequestedFields(request, targetEntity.entityCode(), false);
            if (joinValues.isEmpty()) {
                bundles.put(relation.relationCode(), new RelatedEntityBundle(targetEntity, Map.of(), requestedRelatedFields));
                continue;
            }

            List<String> relatedExecutionFields = ensureFieldPresent(requestedRelatedFields, List.of(relation.toField()));
            RegisteredDataSource relationSource = context.dataSource(targetEntity.sourceId());
            if (relationSource == null) {
                throw new IllegalArgumentException("Datasource not found for related entity: " + targetEntity.entityCode());
            }

            QueryExecutionResult relatedResult = connectorRegistry.get(relationSource.dbType())
                    .execute(new QueryExecutionRequest(
                            targetEntity,
                            List.of(new SearchFilterRequest(relation.toField(), FilterOperator.IN, List.copyOf(joinValues))),
                            relatedExecutionFields,
                            null,
                            null
                    ));

            bundles.put(
                    relation.relationCode(),
                    new RelatedEntityBundle(
                            targetEntity,
                            groupRowsByField(relatedResult.rows(), relation.toField()),
                            requestedRelatedFields
                    )
            );
        }
        return bundles;
    }

    private Set<Object> collectJoinValues(List<Map<String, Object>> rows, String fieldName) {
        Set<Object> joinValues = new HashSet<>();
        for (Map<String, Object> row : rows) {
            Object value = row.get(fieldName);
            if (value != null) {
                joinValues.add(value);
            }
        }
        return joinValues;
    }

    private Map<Object, List<Map<String, Object>>> groupRowsByField(List<Map<String, Object>> rows, String fieldName) {
        Map<Object, List<Map<String, Object>>> grouped = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object value = row.get(fieldName);
            if (value == null) {
                continue;
            }
            grouped.computeIfAbsent(value, ignored -> new ArrayList<>()).add(row);
        }
        return grouped;
    }

    private void attachIncludedRelations(
            Map<String, Object> assembled,
            Map<String, Object> rootRow,
            List<RelationMetadataRecord> relations,
            Map<String, RelatedEntityBundle> includedData
    ) {
        for (RelationMetadataRecord relation : relations) {
            RelatedEntityBundle bundle = includedData.get(relation.relationCode());
            if (bundle == null) {
                continue;
            }

            Object joinValue = rootRow.get(relation.fromField());
            List<Map<String, Object>> matches = joinValue == null
                    ? List.of()
                    : bundle.rowsByJoinValue().getOrDefault(joinValue, List.of());
            List<Map<String, Object>> selectedMatches = matches.stream()
                    .map(row -> selectFields(row, bundle.requestedFields()))
                    .toList();

            if (relation.relationType() == RelationType.ONE_TO_MANY) {
                assembled.put(bundle.entity().entityCode(), selectedMatches);
            } else {
                assembled.put(bundle.entity().entityCode(), selectedMatches.isEmpty() ? null : selectedMatches.get(0));
            }
        }
    }

    private List<String> resolveRequestedFields(SearchRequest request, String entityCode, boolean rootEntity) {
        if (request.entityFields() != null) {
            List<String> entitySpecificFields = request.entityFields().get(entityCode);
            if (entitySpecificFields != null) {
                return entitySpecificFields;
            }
        }
        return rootEntity ? request.fields() : null;
    }

    private List<String> relationSourceFields(List<RelationMetadataRecord> relations) {
        List<String> fields = new ArrayList<>();
        for (RelationMetadataRecord relation : relations) {
            fields.add(relation.fromField());
        }
        return fields;
    }

    private List<String> ensureFieldPresent(List<String> requestedFields, List<String> requiredFields) {
        if (requestedFields == null) {
            return null;
        }
        List<String> resolved = new ArrayList<>(requestedFields);
        for (String requiredField : requiredFields) {
            if (!resolved.contains(requiredField)) {
                resolved.add(requiredField);
            }
        }
        return resolved;
    }

    private Map<String, Object> selectFields(Map<String, Object> row, List<String> requestedFields) {
        if (requestedFields == null || requestedFields.isEmpty()) {
            return row;
        }
        Map<String, Object> selected = new LinkedHashMap<>();
        for (String field : requestedFields) {
            if (row.containsKey(field)) {
                selected.put(field, row.get(field));
            }
        }
        return selected;
    }

    private int normalizeSize(Integer requestedSize) {
        if (requestedSize == null) {
            return searchProperties.getDefaultPageSize();
        }
        return Math.min(Math.max(requestedSize, 1), searchProperties.getMaxPageSize());
    }

    private record RelatedEntityBundle(
            EntityMetadataRecord entity,
            Map<Object, List<Map<String, Object>>> rowsByJoinValue,
            List<String> requestedFields
    ) {
    }
}
