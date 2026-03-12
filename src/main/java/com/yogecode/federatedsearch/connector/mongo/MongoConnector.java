package com.yogecode.federatedsearch.connector.mongo;

import com.yogecode.federatedsearch.common.enums.DatabaseType;
import com.yogecode.federatedsearch.connector.spi.QueryExecutionRequest;
import com.yogecode.federatedsearch.connector.spi.QueryExecutionResult;
import com.yogecode.federatedsearch.connector.spi.SourceConnector;
import org.springframework.stereotype.Component;

@Component
public class MongoConnector implements SourceConnector {

    @Override
    public DatabaseType supports() {
        return DatabaseType.MONGODB;
    }

    @Override
    public QueryExecutionResult execute(QueryExecutionRequest request) {
        throw new IllegalArgumentException("MongoDB query execution is not implemented yet for entity: " + request.entity().entityCode());
    }
}
