package com.yogecode.federatedsearch.datasource.model;

import com.yogecode.federatedsearch.common.enums.DatabaseType;

import java.util.Map;

public record RegisteredDataSource(
        Long id,
        String sourceCode,
        String sourceName,
        DatabaseType dbType,
        String host,
        Integer port,
        String databaseName,
        String username,
        String passwordRef,
        Map<String, Object> connectionParams,
        boolean active
) {
}

