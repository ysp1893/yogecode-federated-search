package com.yogecode.federatedsearch.api.datasource;

import com.yogecode.federatedsearch.common.enums.DatabaseType;

import java.util.Map;

public record DataSourceDetailsResponse(
        Long id,
        String sourceCode,
        String sourceName,
        DatabaseType dbType,
        String host,
        Integer port,
        String databaseName,
        String username,
        Map<String, Object> connectionParams,
        boolean active
) {
}

