package com.yogecode.federatedsearch.search.service;

import com.yogecode.federatedsearch.api.search.SearchFilterRequest;
import com.yogecode.federatedsearch.api.search.SearchRequest;
import com.yogecode.federatedsearch.api.search.SearchResponse;
import com.yogecode.federatedsearch.audit.service.SearchAuditService;
import com.yogecode.federatedsearch.cache.model.SearchMetadataContext;
import com.yogecode.federatedsearch.cache.service.SearchMetadataContextService;
import com.yogecode.federatedsearch.common.enums.DatabaseType;
import com.yogecode.federatedsearch.common.enums.FilterOperator;
import com.yogecode.federatedsearch.common.enums.JoinStrategy;
import com.yogecode.federatedsearch.common.enums.RelationType;
import com.yogecode.federatedsearch.common.enums.StorageType;
import com.yogecode.federatedsearch.config.SearchProperties;
import com.yogecode.federatedsearch.connector.spi.ConnectorRegistry;
import com.yogecode.federatedsearch.connector.spi.QueryExecutionRequest;
import com.yogecode.federatedsearch.connector.spi.QueryExecutionResult;
import com.yogecode.federatedsearch.connector.spi.SourceConnector;
import com.yogecode.federatedsearch.datasource.model.RegisteredDataSource;
import com.yogecode.federatedsearch.metadata.model.EntityMetadataRecord;
import com.yogecode.federatedsearch.metadata.model.RelationMetadataRecord;
import com.yogecode.federatedsearch.planner.SearchPlan;
import com.yogecode.federatedsearch.planner.SearchPlanBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultFederatedSearchServiceTest {

    private final SearchPlanBuilder searchPlanBuilder = mock(SearchPlanBuilder.class);
    private final SearchMetadataContextService contextService = mock(SearchMetadataContextService.class);
    private final ConnectorRegistry connectorRegistry = mock(ConnectorRegistry.class);
    private final SearchAuditService auditService = mock(SearchAuditService.class);

    private final SearchProperties searchProperties = new SearchProperties();

    private final DefaultFederatedSearchService service =
            new DefaultFederatedSearchService(searchPlanBuilder, contextService, connectorRegistry, searchProperties, auditService);

    @AfterEach
    void tearDown() {
        service.shutdownExecutor();
    }

    @Test
    void rejectsRelationFiltersWhenEntityIsNotIncluded() {
        EntityMetadataRecord customer = entity(1L, "customer", 10L, "id");
        SearchPlan plan = new SearchPlan("customer", customer, List.of(), new SearchRequest(
                "customer",
                null,
                List.of(new SearchFilterRequest("cdr.CDRID", FilterOperator.IS_NULL, null)),
                List.of(),
                null,
                null,
                null,
                null,
                0,
                20
        ));

        when(searchPlanBuilder.build(any())).thenReturn(plan);
        when(contextService.getContext("customer")).thenReturn(context(customer, List.of()));

        assertThrows(IllegalArgumentException.class, () -> service.search(plan.request()));
        verify(connectorRegistry, never()).get(any());
    }

    @Test
    void relationFilteredPagingContinuesAcrossRootPages() {
        searchProperties.setDefaultPageSize(2);

        EntityMetadataRecord customer = entity(1L, "customer", 10L, "id");
        EntityMetadataRecord cdr = entity(2L, "cdr", 10L, "CDRID");
        RelationMetadataRecord relation = new RelationMetadataRecord(
                1L,
                "customer_cdr",
                "customer",
                "cdr",
                "username",
                "UserName",
                RelationType.ONE_TO_MANY,
                JoinStrategy.IN,
                true
        );

        SearchRequest request = new SearchRequest(
                "customer",
                null,
                List.of(new SearchFilterRequest("cdr.status", FilterOperator.EQ, "OPEN")),
                List.of("cdr"),
                null,
                null,
                null,
                null,
                0,
                1
        );
        SearchPlan plan = new SearchPlan("customer", customer, List.of(relation), request);

        when(searchPlanBuilder.build(request)).thenReturn(plan);
        when(contextService.getContext("customer")).thenReturn(context(customer, cdr, relation));

        SourceConnector connector = mock(SourceConnector.class);
        when(connectorRegistry.get(DatabaseType.MYSQL)).thenReturn(connector);
        when(connector.execute(any())).thenAnswer(invocation -> executeQuery(invocation.getArgument(0)));

        SearchResponse response = service.search(request);

        assertEquals(1, response.results().size());
        assertEquals(1L, response.total());
        Map<String, Object> root = castMap(response.results().get(0).get("customer"));
        assertEquals(3L, root.get("id"));
    }

    @Test
    void relationIsNullDoesNotMatchWhenRelatedRowsExistWithValues() {
        searchProperties.setDefaultPageSize(2);

        EntityMetadataRecord customer = entity(1L, "customer", 10L, "id");
        EntityMetadataRecord cdr = entity(2L, "cdr", 10L, "CDRID");
        RelationMetadataRecord relation = new RelationMetadataRecord(
                1L,
                "customer_cdr",
                "customer",
                "cdr",
                "username",
                "UserName",
                RelationType.ONE_TO_MANY,
                JoinStrategy.IN,
                true
        );

        SearchRequest request = new SearchRequest(
                "customer",
                null,
                List.of(new SearchFilterRequest("cdr.CDRID", FilterOperator.IS_NULL, null)),
                List.of("cdr"),
                null,
                null,
                null,
                null,
                0,
                10
        );
        SearchPlan plan = new SearchPlan("customer", customer, List.of(relation), request);

        when(searchPlanBuilder.build(request)).thenReturn(plan);
        when(contextService.getContext("customer")).thenReturn(context(customer, cdr, relation));

        SourceConnector connector = mock(SourceConnector.class);
        when(connectorRegistry.get(DatabaseType.MYSQL)).thenReturn(connector);
        when(connector.execute(any())).thenAnswer(invocation -> executeQuery(invocation.getArgument(0)));

        SearchResponse response = service.search(request);

        assertEquals(1, response.results().size());
        assertEquals(1L, response.total());
        Map<String, Object> root = castMap(response.results().get(0).get("customer"));
        assertEquals(2L, root.get("id"));
    }

    @Test
    void relationGreaterThanFilterMatchesExpectedRows() {
        searchProperties.setDefaultPageSize(2);

        EntityMetadataRecord customer = entity(1L, "customer", 10L, "id");
        EntityMetadataRecord cdr = entity(2L, "cdr", 10L, "CDRID");
        RelationMetadataRecord relation = new RelationMetadataRecord(
                1L,
                "customer_cdr",
                "customer",
                "cdr",
                "username",
                "UserName",
                RelationType.ONE_TO_MANY,
                JoinStrategy.IN,
                true
        );

        SearchRequest request = new SearchRequest(
                "customer",
                null,
                List.of(new SearchFilterRequest("cdr.CDRID", FilterOperator.GT, 150L)),
                List.of("cdr"),
                null,
                null,
                null,
                null,
                0,
                10
        );
        SearchPlan plan = new SearchPlan("customer", customer, List.of(relation), request);

        when(searchPlanBuilder.build(request)).thenReturn(plan);
        when(contextService.getContext("customer")).thenReturn(context(customer, cdr, relation));

        SourceConnector connector = mock(SourceConnector.class);
        when(connectorRegistry.get(DatabaseType.MYSQL)).thenReturn(connector);
        when(connector.execute(any())).thenAnswer(invocation -> executeQuery(invocation.getArgument(0)));

        SearchResponse response = service.search(request);

        assertEquals(1, response.results().size());
        assertEquals(1L, response.total());
        Map<String, Object> root = castMap(response.results().get(0).get("customer"));
        assertEquals(3L, root.get("id"));
    }

    @Test
    void rootSearchReturnsTotalAcrossAllPages() {
        searchProperties.setDefaultPageSize(2);

        EntityMetadataRecord customer = entity(1L, "customer", 10L, "id");
        SearchRequest request = new SearchRequest(
                "customer",
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                0,
                1
        );
        SearchPlan plan = new SearchPlan("customer", customer, List.of(), request);

        when(searchPlanBuilder.build(request)).thenReturn(plan);
        when(contextService.getContext("customer")).thenReturn(context(customer, List.of()));

        SourceConnector connector = mock(SourceConnector.class);
        when(connectorRegistry.get(DatabaseType.MYSQL)).thenReturn(connector);
        when(connector.execute(any())).thenAnswer(invocation -> executeQuery(invocation.getArgument(0)));

        SearchResponse response = service.search(request);

        assertEquals(1, response.results().size());
        assertEquals(3L, response.total());
    }

    @Test
    void rootLikeFilterWrapsPlainTextAsContainsSearch() {
        EntityMetadataRecord customer = entity(1L, "customer", 10L, "id");
        SearchRequest request = new SearchRequest(
                "customer",
                null,
                List.of(new SearchFilterRequest("username", FilterOperator.LIKE, "test")),
                List.of(),
                null,
                null,
                null,
                null,
                0,
                20
        );
        SearchPlan plan = new SearchPlan("customer", customer, List.of(), request);

        when(searchPlanBuilder.build(request)).thenReturn(plan);
        when(contextService.getContext("customer")).thenReturn(context(customer, List.of()));

        SourceConnector connector = mock(SourceConnector.class);
        when(connectorRegistry.get(DatabaseType.MYSQL)).thenReturn(connector);
        when(connector.execute(any())).thenAnswer(invocation -> {
            QueryExecutionRequest query = invocation.getArgument(0);
            assertEquals("%test%", query.filters().get(0).value());
            return executeQuery(query);
        });

        service.search(request);
    }

    @Test
    void relationFilteredSearchReturnsMatchedTotalAcrossAllPages() {
        searchProperties.setDefaultPageSize(2);

        EntityMetadataRecord customer = entity(1L, "customer", 10L, "id");
        EntityMetadataRecord cdr = entity(2L, "cdr", 10L, "CDRID");
        RelationMetadataRecord relation = new RelationMetadataRecord(
                1L,
                "customer_cdr",
                "customer",
                "cdr",
                "username",
                "UserName",
                RelationType.ONE_TO_MANY,
                JoinStrategy.IN,
                true
        );

        SearchRequest request = new SearchRequest(
                "customer",
                null,
                List.of(new SearchFilterRequest("cdr.status", FilterOperator.EQ, "OPEN")),
                List.of("cdr"),
                null,
                null,
                null,
                null,
                0,
                1
        );
        SearchPlan plan = new SearchPlan("customer", customer, List.of(relation), request);

        when(searchPlanBuilder.build(request)).thenReturn(plan);
        when(contextService.getContext("customer")).thenReturn(context(customer, cdr, relation));

        SourceConnector connector = mock(SourceConnector.class);
        when(connectorRegistry.get(DatabaseType.MYSQL)).thenReturn(connector);
        when(connector.execute(any())).thenAnswer(invocation -> executeQueryWithTwoOpenRelations(invocation.getArgument(0)));

        SearchResponse response = service.search(request);

        assertEquals(1, response.results().size());
        assertEquals(2L, response.total());
        Map<String, Object> root = castMap(response.results().get(0).get("customer"));
        assertEquals(2L, root.get("id"));
    }

    @Test
    void relationLikeFilterUsesSqlWildcards() {
        searchProperties.setDefaultPageSize(2);

        EntityMetadataRecord customer = entity(1L, "customer", 10L, "id");
        EntityMetadataRecord cdr = entity(2L, "cdr", 10L, "CDRID");
        RelationMetadataRecord relation = new RelationMetadataRecord(
                1L,
                "customer_cdr",
                "customer",
                "cdr",
                "username",
                "UserName",
                RelationType.ONE_TO_MANY,
                JoinStrategy.IN,
                true
        );

        SearchRequest request = new SearchRequest(
                "customer",
                null,
                List.of(new SearchFilterRequest("cdr.status", FilterOperator.LIKE, "%PEN")),
                List.of("cdr"),
                null,
                null,
                null,
                null,
                0,
                10
        );
        SearchPlan plan = new SearchPlan("customer", customer, List.of(relation), request);

        when(searchPlanBuilder.build(request)).thenReturn(plan);
        when(contextService.getContext("customer")).thenReturn(context(customer, cdr, relation));

        SourceConnector connector = mock(SourceConnector.class);
        when(connectorRegistry.get(DatabaseType.MYSQL)).thenReturn(connector);
        when(connector.execute(any())).thenAnswer(invocation -> executeQuery(invocation.getArgument(0)));

        SearchResponse response = service.search(request);

        assertEquals(1, response.results().size());
        Map<String, Object> root = castMap(response.results().get(0).get("customer"));
        assertEquals(3L, root.get("id"));
    }

    @Test
    void relationLikeFilterWrapsPlainTextAsContainsSearch() {
        searchProperties.setDefaultPageSize(2);

        EntityMetadataRecord customer = entity(1L, "customer", 10L, "id");
        EntityMetadataRecord cdr = entity(2L, "cdr", 10L, "CDRID");
        RelationMetadataRecord relation = new RelationMetadataRecord(
                1L,
                "customer_cdr",
                "customer",
                "cdr",
                "username",
                "UserName",
                RelationType.ONE_TO_MANY,
                JoinStrategy.IN,
                true
        );

        SearchRequest request = new SearchRequest(
                "customer",
                null,
                List.of(new SearchFilterRequest("cdr.status", FilterOperator.LIKE, "PEN")),
                List.of("cdr"),
                null,
                null,
                null,
                null,
                0,
                10
        );
        SearchPlan plan = new SearchPlan("customer", customer, List.of(relation), request);

        when(searchPlanBuilder.build(request)).thenReturn(plan);
        when(contextService.getContext("customer")).thenReturn(context(customer, cdr, relation));

        SourceConnector connector = mock(SourceConnector.class);
        when(connectorRegistry.get(DatabaseType.MYSQL)).thenReturn(connector);
        when(connector.execute(any())).thenAnswer(invocation -> executeQuery(invocation.getArgument(0)));

        SearchResponse response = service.search(request);

        assertEquals(1, response.results().size());
        assertTrue(response.total() >= 1L);
        Map<String, Object> root = castMap(response.results().get(0).get("customer"));
        assertEquals(3L, root.get("id"));
    }

    @Test
    void rejectsAmbiguousRelationFiltersWhenMultipleIncludedRelationsTargetSameEntity() {
        EntityMetadataRecord customer = entity(1L, "customer", 10L, "id");
        EntityMetadataRecord cdr = entity(2L, "cdr", 10L, "CDRID");
        RelationMetadataRecord usernameRelation = new RelationMetadataRecord(
                1L,
                "customer_cdr_username",
                "customer",
                "cdr",
                "username",
                "UserName",
                RelationType.ONE_TO_MANY,
                JoinStrategy.IN,
                true
        );
        RelationMetadataRecord emailRelation = new RelationMetadataRecord(
                2L,
                "customer_cdr_email",
                "customer",
                "cdr",
                "email",
                "Email",
                RelationType.ONE_TO_MANY,
                JoinStrategy.IN,
                true
        );

        SearchRequest request = new SearchRequest(
                "customer",
                null,
                List.of(new SearchFilterRequest("cdr.status", FilterOperator.EQ, "OPEN")),
                List.of("cdr"),
                null,
                null,
                null,
                null,
                0,
                20
        );
        SearchPlan plan = new SearchPlan("customer", customer, List.of(usernameRelation, emailRelation), request);

        when(searchPlanBuilder.build(request)).thenReturn(plan);
        when(contextService.getContext("customer")).thenReturn(context(customer, cdr, usernameRelation, emailRelation));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.search(request));

        assertEquals(
                "Relation filter is ambiguous for entity 'cdr'. Multiple included relations target that entity.",
                exception.getMessage()
        );
        verify(connectorRegistry, never()).get(any());
    }

    private SearchMetadataContext context(EntityMetadataRecord customer, List<RelationMetadataRecord> relations) {
        RegisteredDataSource source = new RegisteredDataSource(
                10L, "main", "Main", DatabaseType.MYSQL, "localhost", 3306, "db", "user", "{noop}pw", Map.of(), true
        );
        return new SearchMetadataContext(
                "customer",
                customer,
                source,
                relations,
                Map.of("customer", customer),
                Map.of(10L, source)
        );
    }

    private SearchMetadataContext context(
            EntityMetadataRecord customer,
            EntityMetadataRecord cdr,
            RelationMetadataRecord relation
    ) {
        RegisteredDataSource source = new RegisteredDataSource(
                10L, "main", "Main", DatabaseType.MYSQL, "localhost", 3306, "db", "user", "{noop}pw", Map.of(), true
        );
        return new SearchMetadataContext(
                "customer",
                customer,
                source,
                List.of(relation),
                Map.of("customer", customer, "cdr", cdr),
                Map.of(10L, source)
        );
    }

    private SearchMetadataContext context(
            EntityMetadataRecord customer,
            EntityMetadataRecord cdr,
            RelationMetadataRecord firstRelation,
            RelationMetadataRecord secondRelation
    ) {
        RegisteredDataSource source = new RegisteredDataSource(
                10L, "main", "Main", DatabaseType.MYSQL, "localhost", 3306, "db", "user", "{noop}pw", Map.of(), true
        );
        return new SearchMetadataContext(
                "customer",
                customer,
                source,
                List.of(firstRelation, secondRelation),
                Map.of("customer", customer, "cdr", cdr),
                Map.of(10L, source)
        );
    }

    private EntityMetadataRecord entity(Long id, String code, Long sourceId, String primaryKey) {
        return new EntityMetadataRecord(
                id,
                code,
                code,
                sourceId,
                StorageType.TABLE,
                "public",
                code,
                primaryKey,
                code,
                true,
                true
        );
    }

    private QueryExecutionResult executeQuery(QueryExecutionRequest request) {
        if ("customer".equals(request.entity().entityCode())) {
            List<Map<String, Object>> allRows = List.of(
                    Map.of("id", 1L, "username", "alpha"),
                    Map.of("id", 2L, "username", "beta"),
                    Map.of("id", 3L, "username", "gamma")
            );
            int page = request.page() == null ? 0 : request.page();
            int size = request.size() == null ? allRows.size() : request.size();
            int fromIndex = Math.min(page * size, allRows.size());
            int toIndex = Math.min(fromIndex + size, allRows.size());
            return new QueryExecutionResult("customer", allRows.subList(fromIndex, toIndex), allRows.size());
        }

        List<?> joinValues = (List<?>) request.filters().get(0).value();
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        if (joinValues.contains("alpha")) {
            rows.add(Map.of("UserName", "alpha", "status", "CLOSED", "CDRID", 100L));
        }
        if (joinValues.contains("beta")) {
            Map<String, Object> betaRow = new java.util.LinkedHashMap<>();
            betaRow.put("UserName", "beta");
            betaRow.put("status", null);
            betaRow.put("CDRID", null);
            rows.add(betaRow);
        }
        if (joinValues.contains("gamma")) {
            rows.add(Map.of("UserName", "gamma", "status", "OPEN", "CDRID", 200L));
        }
        return new QueryExecutionResult("cdr", rows, rows.size());
    }

    private QueryExecutionResult executeQueryWithTwoOpenRelations(QueryExecutionRequest request) {
        if ("customer".equals(request.entity().entityCode())) {
            List<Map<String, Object>> allRows = List.of(
                    Map.of("id", 1L, "username", "alpha"),
                    Map.of("id", 2L, "username", "beta"),
                    Map.of("id", 3L, "username", "gamma")
            );
            int page = request.page() == null ? 0 : request.page();
            int size = request.size() == null ? allRows.size() : request.size();
            int fromIndex = Math.min(page * size, allRows.size());
            int toIndex = Math.min(fromIndex + size, allRows.size());
            return new QueryExecutionResult("customer", allRows.subList(fromIndex, toIndex), allRows.size());
        }

        List<?> joinValues = (List<?>) request.filters().get(0).value();
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        if (joinValues.contains("alpha")) {
            rows.add(Map.of("UserName", "alpha", "status", "CLOSED", "CDRID", 100L));
        }
        if (joinValues.contains("beta")) {
            rows.add(Map.of("UserName", "beta", "status", "OPEN", "CDRID", 200L));
        }
        if (joinValues.contains("gamma")) {
            rows.add(Map.of("UserName", "gamma", "status", "OPEN", "CDRID", 300L));
        }
        return new QueryExecutionResult("cdr", rows, rows.size());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
