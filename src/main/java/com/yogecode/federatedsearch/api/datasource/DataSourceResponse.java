package com.yogecode.federatedsearch.api.datasource;

import com.yogecode.federatedsearch.common.enums.DatabaseType;

public record DataSourceResponse(
        Long id,
        String sourceCode,
        String sourceName,
        DatabaseType dbType,
        String status
) {
}

