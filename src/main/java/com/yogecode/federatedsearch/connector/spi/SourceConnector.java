package com.yogecode.federatedsearch.connector.spi;

import com.yogecode.federatedsearch.common.enums.DatabaseType;

public interface SourceConnector {

    DatabaseType supports();

    QueryExecutionResult execute(QueryExecutionRequest request);
}

