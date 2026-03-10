package com.yogecode.federatedsearch.connector.sql;

import com.yogecode.federatedsearch.common.enums.DatabaseType;
import com.yogecode.federatedsearch.connector.spi.QueryExecutionRequest;
import com.yogecode.federatedsearch.connector.spi.QueryExecutionResult;
import com.yogecode.federatedsearch.connector.spi.SourceConnector;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PostgresConnector implements SourceConnector {

    @Override
    public DatabaseType supports() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    public QueryExecutionResult execute(QueryExecutionRequest request) {
        return new QueryExecutionResult(request.entity().entityCode(), List.of(Map.of(
                "sourceType", "POSTGRESQL",
                "objectName", request.entity().objectName(),
                "message", "Query execution not implemented yet"
        )));
    }
}

