package com.yogecode.federatedsearch.api.datasource;

import com.yogecode.federatedsearch.common.enums.DatabaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateDataSourceRequest(
        @NotBlank String sourceCode,
        @NotBlank String sourceName,
        @NotNull DatabaseType dbType,
        @NotBlank String host,
        @NotNull Integer port,
        @NotBlank String databaseName,
        @NotBlank String username,
        @NotBlank String password,
        Map<String, Object> connectionParams
) {
}

