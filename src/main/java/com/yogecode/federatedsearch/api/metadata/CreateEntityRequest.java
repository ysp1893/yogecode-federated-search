package com.yogecode.federatedsearch.api.metadata;

import com.yogecode.federatedsearch.common.enums.StorageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEntityRequest(
        @NotBlank String entityCode,
        @NotBlank String entityName,
        @NotNull Long sourceId,
        @NotNull StorageType storageType,
        String objectSchema,
        @NotBlank String objectName,
        String primaryKeyField,
        @NotBlank String businessLabel,
        boolean rootSearchable
) {
}

