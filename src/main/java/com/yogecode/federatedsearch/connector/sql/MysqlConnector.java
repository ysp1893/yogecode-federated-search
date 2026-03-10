package com.yogecode.federatedsearch.connector.sql;

import com.yogecode.federatedsearch.api.search.SearchFilterRequest;
import com.yogecode.federatedsearch.common.enums.DatabaseType;
import com.yogecode.federatedsearch.common.enums.FilterOperator;
import com.yogecode.federatedsearch.connector.spi.QueryExecutionRequest;
import com.yogecode.federatedsearch.connector.spi.QueryExecutionResult;
import com.yogecode.federatedsearch.connector.spi.SourceConnector;
import com.yogecode.federatedsearch.datasource.model.RegisteredDataSource;
import com.yogecode.federatedsearch.datasource.service.DataSourceService;
import com.yogecode.federatedsearch.metadata.model.EntityMetadataRecord;
import com.yogecode.federatedsearch.security.CredentialCipher;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MysqlConnector implements SourceConnector {

    private static final String IDENTIFIER_PATTERN = "[A-Za-z0-9_]+";

    private final DataSourceService dataSourceService;
    private final CredentialCipher credentialCipher;

    public MysqlConnector(
            DataSourceService dataSourceService,
            CredentialCipher credentialCipher
    ) {
        this.dataSourceService = dataSourceService;
        this.credentialCipher = credentialCipher;
    }

    @Override
    public DatabaseType supports() {
        return DatabaseType.MYSQL;
    }

    @Override
    public QueryExecutionResult execute(QueryExecutionRequest request) {
        RegisteredDataSource dataSource = dataSourceService.findById(request.entity().sourceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Datasource not found for sourceId: " + request.entity().sourceId()
                ));

        String tableName = buildQualifiedObjectName(request.entity());
        String selectClause = buildSelectClause(request.fields());
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(selectClause)
                .append(" FROM ")
                .append(tableName);
        List<Object> parameters = new ArrayList<>();

        appendWhereClause(sql, request.filters(), parameters);
        appendPagination(sql, parameters, request.page(), request.size());

        try (Connection connection = DriverManager.getConnection(
                buildJdbcUrl(dataSource),
                dataSource.username(),
                resolvePassword(dataSource.passwordRef())
        ); PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            bindParameters(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) {
                return new QueryExecutionResult(request.entity().entityCode(), extractRows(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to execute MySQL query for entity: " + request.entity().entityCode(), ex);
        }
    }

    private String buildSelectClause(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return "*";
        }
        List<String> safeFields = new ArrayList<>();
        for (String field : fields) {
            safeFields.add(quoteSimpleIdentifier(field));
        }
        return String.join(", ", safeFields);
    }

    private void appendWhereClause(StringBuilder sql, List<SearchFilterRequest> filters, List<Object> parameters) {
        if (filters == null || filters.isEmpty()) {
            return;
        }

        sql.append(" WHERE ");
        List<String> predicates = new ArrayList<>();
        for (SearchFilterRequest filter : filters) {
            String field = quoteSimpleIdentifier(filter.field());
            if (filter.operator() == FilterOperator.EQ) {
                predicates.add(field + " = ?");
                parameters.add(filter.value());
            } else if (filter.operator() == FilterOperator.NE) {
                predicates.add(field + " <> ?");
                parameters.add(filter.value());
            } else if (filter.operator() == FilterOperator.LIKE) {
                predicates.add(field + " LIKE ?");
                parameters.add(String.valueOf(filter.value()));
            } else if (filter.operator() == FilterOperator.IN) {
                appendCollectionPredicate(predicates, parameters, field, filter.value(), false);
            } else if (filter.operator() == FilterOperator.NOT_IN) {
                appendCollectionPredicate(predicates, parameters, field, filter.value(), true);
            } else if (filter.operator() == FilterOperator.IS_NULL) {
                predicates.add(field + " IS NULL");
            } else if (filter.operator() == FilterOperator.IS_NOT_NULL) {
                predicates.add(field + " IS NOT NULL");
            } else {
                throw new IllegalArgumentException("Unsupported filter operator: " + filter.operator());
            }
        }
        sql.append(String.join(" AND ", predicates));
    }

    private void appendCollectionPredicate(
            List<String> predicates,
            List<Object> parameters,
            String field,
            Object rawValue,
            boolean negative
    ) {
        List<Object> values = normalizeInValues(rawValue);
        if (values.isEmpty()) {
            predicates.add(negative ? "1 = 1" : "1 = 0");
            return;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(values.size(), "?"));
        predicates.add(field + (negative ? " NOT IN (" : " IN (") + placeholders + ")");
        parameters.addAll(values);
    }

    private void appendPagination(StringBuilder sql, List<Object> parameters, Integer page, Integer size) {
        if (size == null) {
            return;
        }
        int safeSize = Math.max(size, 1);
        int safePage = page == null ? 0 : Math.max(page, 0);
        sql.append(" LIMIT ? OFFSET ?");
        parameters.add(safeSize);
        parameters.add(safePage * safeSize);
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }

    private List<Map<String, Object>> extractRows(ResultSet rs) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(metaData.getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }

    private String buildJdbcUrl(RegisteredDataSource source) {
        Object jdbcUrl = source.connectionParams() == null ? null : source.connectionParams().get("jdbcUrl");
        if (jdbcUrl != null && !String.valueOf(jdbcUrl).isBlank()) {
            return String.valueOf(jdbcUrl);
        }
        return "jdbc:mysql://" + source.host() + ":" + source.port() + "/" + source.databaseName();
    }

    private String resolvePassword(String passwordRef) {
        if (passwordRef == null) {
            return null;
        }
        String decrypted = credentialCipher.decrypt(passwordRef);
        if (decrypted != null && decrypted.startsWith("{noop}")) {
            return decrypted.substring("{noop}".length());
        }
        return decrypted;
    }

    private String buildQualifiedObjectName(EntityMetadataRecord entity) {
        if (entity.objectSchema() == null || entity.objectSchema().isBlank()) {
            return quoteQualifiedIdentifier(entity.objectName());
        }
        return quoteQualifiedIdentifier(entity.objectSchema()) + "." + quoteQualifiedIdentifier(entity.objectName());
    }

    private String quoteQualifiedIdentifier(String identifier) {
        String[] parts = identifier.split("\\.");
        List<String> safeParts = new ArrayList<>();
        for (String part : parts) {
            safeParts.add(quoteSimpleIdentifier(part));
        }
        return String.join(".", safeParts);
    }

    private String quoteSimpleIdentifier(String identifier) {
        if (identifier == null || !identifier.matches(IDENTIFIER_PATTERN)) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + identifier);
        }
        return "`" + identifier + "`";
    }

    private List<Object> normalizeInValues(Object rawValue) {
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
}
