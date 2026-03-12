package com.yogecode.federatedsearch.connector.sql;

import com.yogecode.federatedsearch.common.enums.DatabaseType;
import com.yogecode.federatedsearch.connector.spi.QueryExecutionRequest;
import com.yogecode.federatedsearch.connector.spi.QueryExecutionResult;
import com.yogecode.federatedsearch.connector.spi.SourceConnector;
import org.springframework.stereotype.Component;

@Component
public class PostgresConnector implements SourceConnector {

    @Override
    public DatabaseType supports() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    public QueryExecutionResult execute(QueryExecutionRequest request) {
        throw new IllegalArgumentException("PostgreSQL query execution is not implemented yet for entity: " + request.entity().entityCode());
    }
}
