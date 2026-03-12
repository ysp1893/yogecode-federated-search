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
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.math.BigDecimal;
import java.util.regex.Pattern;

@Service
public class DefaultFederatedSearchService implements FederatedSearchService {

    private final ExecutorService relationExecutor;

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
        this.relationExecutor = Executors.newFixedThreadPool(searchProperties.getRelationExecutorThreads());
    }

    @Override
    public SearchResponse search(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        SearchPlan plan = searchPlanBuilder.build(request);
        SearchMetadataContext context = searchMetadataContextService.getContext(plan.rootEntityCode());

        int page = request.page() == null ? 0 : Math.max(request.page(), 0);
        int size = normalizeSize(request.size());

        List<SearchFilterRequest> rootFilters = rootFilters(request.filters(), plan.rootEntityCode());
        List<SearchFilterRequest> relationFilters = relationFilters(request.filters(), plan.rootEntityCode());
        validateRelationFiltersIncluded(plan, relationFilters);
        validateRelationFiltersUnambiguous(plan, relationFilters);

        List<String> requestedRootFields = resolveRequestedFields(request, plan.rootEntityCode(), true);
        List<String> rootExecutionFields = ensureFieldPresent(
                requestedRootFields,
                relationSourceFields(plan.includedRelations(), relationFilters)
        );

        String rootSortBy = resolveRootSortBy(request, plan.rootEntity());
        String rootSortDirection = resolveSortDirection(request.sortDirection());

        SearchExecutionOutcome outcome = relationFilters.isEmpty()
                ? searchWithoutRelationPaging(context, plan, request, rootFilters, relationFilters,
                requestedRootFields, rootExecutionFields, rootSortBy, rootSortDirection, page, size)
                : searchWithRelationPaging(context, plan, request, rootFilters, relationFilters,
                requestedRootFields, rootExecutionFields, rootSortBy, rootSortDirection, page, size);

        SearchResponse response = new SearchResponse(
                UUID.randomUUID().toString(),
                plan.rootEntityCode(),
                page,
                size,
                outcome.total(),
                outcome.results(),
                List.<PartialFailureResponse>of()
        );

        searchAuditService.record(request, response, System.currentTimeMillis() - startTime);

        return response;
    }

    @PreDestroy
    void shutdownExecutor() {
        relationExecutor.shutdown();
    }

    private SearchExecutionOutcome searchWithoutRelationPaging(
            SearchMetadataContext context,
            SearchPlan plan,
            SearchRequest request,
            List<SearchFilterRequest> rootFilters,
            List<SearchFilterRequest> relationFilters,
            List<String> requestedRootFields,
            List<String> rootExecutionFields,
            String rootSortBy,
            String rootSortDirection,
            int page,
            int size
    ) {
        QueryExecutionResult rootResult = executeRootQuery(
                context.rootSource(),
                plan.rootEntity(),
                rootFilters,
                rootExecutionFields,
                rootSortBy,
                rootSortDirection,
                page,
                size
        );

        Map<String, RelatedEntityBundle> includedData = loadIncludedRelations(context, plan, request, rootResult.rows());
        return new SearchExecutionOutcome(
                assembleResults(plan, requestedRootFields, relationFilters, rootResult.rows(), includedData),
                rootResult.total()
        );
    }

    private SearchExecutionOutcome searchWithRelationPaging(
            SearchMetadataContext context,
            SearchPlan plan,
            SearchRequest request,
            List<SearchFilterRequest> rootFilters,
            List<SearchFilterRequest> relationFilters,
            List<String> requestedRootFields,
            List<String> rootExecutionFields,
            String rootSortBy,
            String rootSortDirection,
            int page,
            int size
    ) {
        List<Map<String, Object>> pagedResults = new ArrayList<>();
        long matchedRows = 0;
        int skippedMatches = page * size;
        int sourcePage = 0;
        int batchSize = Math.max(size, searchProperties.getDefaultPageSize());

        while (true) {
            QueryExecutionResult rootBatch = executeRootQuery(
                    context.rootSource(),
                    plan.rootEntity(),
                    rootFilters,
                    rootExecutionFields,
                    rootSortBy,
                    rootSortDirection,
                    sourcePage,
                    batchSize
            );

            List<Map<String, Object>> rootRows = rootBatch.rows();
            if (rootRows.isEmpty()) {
                break;
            }

            Map<String, RelatedEntityBundle> includedData = loadIncludedRelations(context, plan, request, rootRows);

            for (Map<String, Object> row : rootRows) {
                if (!matchesRelationFilters(row, relationFilters, includedData)) {
                    continue;
                }
                if (matchedRows++ < skippedMatches) {
                    continue;
                }

                if (pagedResults.size() < size) {
                    pagedResults.add(assembleResult(plan, requestedRootFields, row, includedData));
                }
            }

            if (rootRows.size() < batchSize) {
                break;
            }

            sourcePage++;
        }

        return new SearchExecutionOutcome(pagedResults, matchedRows);
    }

    private QueryExecutionResult executeRootQuery(
            RegisteredDataSource rootSource,
            EntityMetadataRecord rootEntity,
            List<SearchFilterRequest> rootFilters,
            List<String> rootExecutionFields,
            String rootSortBy,
            String rootSortDirection,
            Integer page,
            Integer size
    ) {
        return connectorRegistry.get(rootSource.dbType())
                .execute(new QueryExecutionRequest(
                        rootEntity,
                        rootFilters,
                        rootExecutionFields,
                        rootSortBy,
                        rootSortDirection,
                        page,
                        size
                ));
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

        Map<String, CompletableFuture<RelatedEntityBundle>> futures = new HashMap<>();

        for (RelationMetadataRecord relation : plan.includedRelations()) {
            CompletableFuture<RelatedEntityBundle> future = CompletableFuture.supplyAsync(() -> {
                EntityMetadataRecord targetEntity = context.entity(relation.toEntityCode());

                if (targetEntity == null) {
                    throw new IllegalArgumentException("Unknown related entity: " + relation.toEntityCode());
                }

                Set<Object> joinValues = collectJoinValues(rootRows, relation.fromField());
                List<String> requestedRelatedFields = resolveRequestedFields(request, targetEntity.entityCode(), false);
                List<String> requiredFields = requiredRelatedFields(relation, plan.includedRelations(), request.filters());

                if (joinValues.isEmpty()) {
                    return new RelatedEntityBundle(targetEntity, relation, Map.of(), requestedRelatedFields);
                }

                List<String> relatedExecutionFields = ensureFieldPresent(requestedRelatedFields, requiredFields);
                RegisteredDataSource relationSource = context.dataSource(targetEntity.sourceId());
                List<Map<String, Object>> allRows = new ArrayList<>();
                int batchSize = searchProperties.getJoinBatchSize();

                for (List<Object> batch : batchValues(joinValues, batchSize)) {
                    List<SearchFilterRequest> filters = List.of(
                            new SearchFilterRequest(relation.toField(), FilterOperator.IN, batch)
                    );

                    QueryExecutionResult batchResult = connectorRegistry.get(relationSource.dbType())
                            .execute(new QueryExecutionRequest(
                                    targetEntity,
                                    filters,
                                    relatedExecutionFields,
                                    null,
                                    null,
                                    null,
                                    null
                            ));

                    allRows.addAll(batchResult.rows());
                }

                return new RelatedEntityBundle(
                        targetEntity,
                        relation,
                        groupRowsByField(allRows, relation.toField()),
                        requestedRelatedFields
                );
            }, relationExecutor);

            futures.put(relation.relationCode(), future);
        }

        Map<String, RelatedEntityBundle> bundles = new HashMap<>();

        for (Map.Entry<String, CompletableFuture<RelatedEntityBundle>> entry : futures.entrySet()) {
            bundles.put(entry.getKey(), entry.getValue().join());
        }

        return bundles;
    }

    private List<SearchFilterRequest> rootFilters(List<SearchFilterRequest> filters, String rootEntityCode) {
        if (filters == null || filters.isEmpty()) {
            return List.of();
        }
        return filters.stream()
                .filter(filter -> !isRelationFilter(filter, rootEntityCode))
                .map(filter -> normalizeRootFilter(filter, rootEntityCode))
                .toList();
    }

    private SearchFilterRequest normalizeRootFilter(SearchFilterRequest filter, String rootEntityCode) {
        EntityScopedField scopedField = parseScopedField(filter.field(), rootEntityCode);
        return new SearchFilterRequest(
                scopedField.fieldName(),
                filter.operator(),
                normalizeFilterValue(filter.operator(), filter.value())
        );
    }

    private List<SearchFilterRequest> relationFilters(List<SearchFilterRequest> filters, String rootEntityCode) {
        if (filters == null || filters.isEmpty()) {
            return List.of();
        }
        return filters.stream()
                .filter(filter -> isRelationFilter(filter, rootEntityCode))
                .map(filter -> new SearchFilterRequest(
                        filter.field(),
                        filter.operator(),
                        normalizeFilterValue(filter.operator(), filter.value())
                ))
                .toList();
    }

    private boolean isRelationFilter(SearchFilterRequest filter, String rootEntityCode) {
        EntityScopedField scopedField = parseScopedField(filter.field(), rootEntityCode);
        return scopedField.entityCode() != null && !scopedField.entityCode().equals(rootEntityCode);
    }

    private boolean matchesRelationFilters(
            Map<String, Object> rootRow,
            List<SearchFilterRequest> relationFilters,
            Map<String, RelatedEntityBundle> includedData
    ) {
        for (SearchFilterRequest filter : relationFilters) {
            if (!matchesRelationFilter(rootRow, filter, includedData)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesRelationFilter(
            Map<String, Object> rootRow,
            SearchFilterRequest filter,
            Map<String, RelatedEntityBundle> includedData
    ) {
        EntityScopedField scopedField = parseScopedField(filter.field(), null);
        List<Map<String, Object>> relatedRows = relatedRowsFor(rootRow, scopedField.entityCode(), includedData);
        if (filter.operator() == FilterOperator.IS_NULL) {
            return relatedRows.isEmpty() || relatedRows.stream().anyMatch(row -> row.get(scopedField.fieldName()) == null);
        }
        if (filter.operator() == FilterOperator.IS_NOT_NULL) {
            return relatedRows.stream().anyMatch(row -> row.get(scopedField.fieldName()) != null);
        }
        if (relatedRows.isEmpty()) {
            return false;
        }
        return relatedRows.stream().anyMatch(row -> matchesFieldValue(row.get(scopedField.fieldName()), filter));
    }

    private List<Map<String, Object>> relatedRowsFor(
            Map<String, Object> rootRow,
            String entityCode,
            Map<String, RelatedEntityBundle> includedData
    ) {
        for (RelatedEntityBundle bundle : includedData.values()) {
            if (!bundle.entity().entityCode().equals(entityCode)) {
                continue;
            }
            Object joinValue = rootRow.get(bundle.relation().fromField());
            if (joinValue == null) {
                return List.of();
            }
            return bundle.rowsByJoinValue().getOrDefault(joinValue, List.of());
        }
        return List.of();
    }

    private boolean matchesFieldValue(Object fieldValue, SearchFilterRequest filter) {
        if (requiresComparison(filter.operator()) && (fieldValue == null || filter.value() == null)) {
            return false;
        }
        return switch (filter.operator()) {
            case EQ -> Objects.equals(fieldValue, filter.value());
            case NE -> !Objects.equals(fieldValue, filter.value());
            case GT -> compareValues(fieldValue, filter.value()) > 0;
            case GTE -> compareValues(fieldValue, filter.value()) >= 0;
            case LT -> compareValues(fieldValue, filter.value()) < 0;
            case LTE -> compareValues(fieldValue, filter.value()) <= 0;
            case LIKE -> fieldValue != null && likePatternMatches(fieldValue, filter.value());
            case IN -> normalizeValues(filter.value()).contains(fieldValue);
            case NOT_IN -> !normalizeValues(filter.value()).contains(fieldValue);
            case IS_NULL -> fieldValue == null;
            case IS_NOT_NULL -> fieldValue != null;
        };
    }

    private boolean requiresComparison(FilterOperator operator) {
        return operator == FilterOperator.GT
                || operator == FilterOperator.GTE
                || operator == FilterOperator.LT
                || operator == FilterOperator.LTE;
    }

    private int compareValues(Object left, Object right) {
        if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
            return BigDecimal.valueOf(leftNumber.doubleValue())
                    .compareTo(BigDecimal.valueOf(rightNumber.doubleValue()));
        }
        String leftValue = String.valueOf(left);
        String rightValue = String.valueOf(right);
        return leftValue.compareTo(rightValue);
    }

    private boolean likePatternMatches(Object fieldValue, Object rawPattern) {
        if (rawPattern == null) {
            return false;
        }
        String pattern = String.valueOf(rawPattern);
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (ch == '%') {
                regex.append(".*");
            } else if (ch == '_') {
                regex.append('.');
            } else {
                regex.append(Pattern.quote(String.valueOf(ch)));
            }
        }
        regex.append('$');
        return Pattern.compile(regex.toString(), Pattern.DOTALL)
                .matcher(String.valueOf(fieldValue))
                .matches();
    }

    private Object normalizeFilterValue(FilterOperator operator, Object value) {
        if (operator != FilterOperator.LIKE || !(value instanceof String stringValue)) {
            return value;
        }
        if (stringValue.contains("%") || stringValue.contains("_")) {
            return stringValue;
        }
        return "%" + stringValue + "%";
    }

    private EntityScopedField parseScopedField(String rawField, String defaultEntityCode) {
        if (rawField != null && rawField.contains(".")) {
            String[] parts = rawField.split("\\.", 2);
            return new EntityScopedField(parts[0], parts[1]);
        }
        return new EntityScopedField(defaultEntityCode, rawField);
    }

    private List<Object> normalizeValues(Object rawValue) {
        if (rawValue == null) {
            return List.of();
        }
        if (rawValue instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (rawValue.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(rawValue);
            List<Object> values = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                values.add(java.lang.reflect.Array.get(rawValue, i));
            }
            return values;
        }
        return List.of(rawValue);
    }

    private List<List<Object>> batchValues(Set<Object> values, int batchSize) {
        List<List<Object>> batches = new ArrayList<>();
        List<Object> current = new ArrayList<>(batchSize);

        for (Object value : values) {
            current.add(value);

            if (current.size() == batchSize) {
                batches.add(new ArrayList<>(current));
                current.clear();
            }
        }

        if (!current.isEmpty()) {
            batches.add(new ArrayList<>(current));
        }

        return batches;
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

    private String resolveRootSortBy(SearchRequest request, EntityMetadataRecord rootEntity) {
        if (request.sortBy() != null && !request.sortBy().isBlank()) {
            EntityScopedField scopedField = parseScopedField(request.sortBy(), rootEntity.entityCode());
            if (scopedField.entityCode() != null && !scopedField.entityCode().equals(rootEntity.entityCode())) {
                throw new IllegalArgumentException("Sorting is supported only on the root entity for now: " + request.sortBy());
            }
            return scopedField.fieldName();
        }
        return rootEntity.primaryKeyField();
    }

    private String resolveSortDirection(String sortDirection) {
        if (sortDirection == null || sortDirection.isBlank()) {
            return "ASC";
        }
        return sortDirection;
    }

    private List<String> relationSourceFields(List<RelationMetadataRecord> relations, List<SearchFilterRequest> relationFilters) {
        List<String> fields = new ArrayList<>();
        for (RelationMetadataRecord relation : relations) {
            fields.add(relation.fromField());
        }
        for (SearchFilterRequest relationFilter : relationFilters) {
            EntityScopedField scopedField = parseScopedField(relationFilter.field(), null);
            if (scopedField.entityCode() != null && scopedField.fieldName() != null) {
                for (RelationMetadataRecord relation : relations) {
                    if (relation.toEntityCode().equals(scopedField.entityCode()) && !fields.contains(relation.fromField())) {
                        fields.add(relation.fromField());
                    }
                }
            }
        }
        return fields;
    }

    private List<String> requiredRelatedFields(
            RelationMetadataRecord relation,
            List<RelationMetadataRecord> relations,
            List<SearchFilterRequest> filters
    ) {
        List<String> requiredFields = new ArrayList<>();
        requiredFields.add(relation.toField());

        if (filters == null || filters.isEmpty()) {
            return requiredFields;
        }

        for (SearchFilterRequest filter : filters) {
            EntityScopedField scopedField = parseScopedField(filter.field(), null);
            if (scopedField.entityCode() == null || scopedField.fieldName() == null) {
                continue;
            }
            for (RelationMetadataRecord candidate : relations) {
                if (candidate.toEntityCode().equals(scopedField.entityCode())
                        && candidate.relationCode().equals(relation.relationCode())
                        && !requiredFields.contains(scopedField.fieldName())) {
                    requiredFields.add(scopedField.fieldName());
                }
            }
        }

        return requiredFields;
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

    private void validateRelationFiltersIncluded(SearchPlan plan, List<SearchFilterRequest> relationFilters) {
        Set<String> includedEntities = new HashSet<>();
        for (RelationMetadataRecord relation : plan.includedRelations()) {
            includedEntities.add(relation.toEntityCode());
        }

        for (SearchFilterRequest relationFilter : relationFilters) {
            EntityScopedField scopedField = parseScopedField(relationFilter.field(), null);
            if (scopedField.entityCode() != null && !includedEntities.contains(scopedField.entityCode())) {
                throw new IllegalArgumentException(
                        "Relation filters require the related entity in include: " + scopedField.entityCode());
            }
        }
    }

    private void validateRelationFiltersUnambiguous(SearchPlan plan, List<SearchFilterRequest> relationFilters) {
        for (SearchFilterRequest relationFilter : relationFilters) {
            EntityScopedField scopedField = parseScopedField(relationFilter.field(), null);
            if (scopedField.entityCode() == null) {
                continue;
            }
            long matchingRelations = plan.includedRelations().stream()
                    .filter(relation -> relation.toEntityCode().equals(scopedField.entityCode()))
                    .count();
            if (matchingRelations > 1) {
                throw new IllegalArgumentException(
                        "Relation filter is ambiguous for entity '"
                                + scopedField.entityCode()
                                + "'. Multiple included relations target that entity.");
            }
        }
    }

    private List<Map<String, Object>> assembleResults(
            SearchPlan plan,
            List<String> requestedRootFields,
            List<SearchFilterRequest> relationFilters,
            List<Map<String, Object>> rootRows,
            Map<String, RelatedEntityBundle> includedData
    ) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> row : rootRows) {
            if (!matchesRelationFilters(row, relationFilters, includedData)) {
                continue;
            }
            results.add(assembleResult(plan, requestedRootFields, row, includedData));
        }
        return results;
    }

    private Map<String, Object> assembleResult(
            SearchPlan plan,
            List<String> requestedRootFields,
            Map<String, Object> row,
            Map<String, RelatedEntityBundle> includedData
    ) {
        Map<String, Object> assembled = new LinkedHashMap<>();
        assembled.put(plan.rootEntityCode(), selectFields(row, requestedRootFields));
        attachIncludedRelations(assembled, row, plan.includedRelations(), includedData);
        return assembled;
    }

    private record RelatedEntityBundle(
            EntityMetadataRecord entity,
            RelationMetadataRecord relation,
            Map<Object, List<Map<String, Object>>> rowsByJoinValue,
            List<String> requestedFields
    ) {
    }

    private record SearchExecutionOutcome(
            List<Map<String, Object>> results,
            long total
    ) {
    }

    private record EntityScopedField(
            String entityCode,
            String fieldName
    ) {
    }
}
