package com.yogecode.federatedsearch.api.metadata;

import jakarta.validation.constraints.NotBlank;

public record FieldRequest(
        @NotBlank String fieldName,
        String fieldPath,
        @NotBlank String dataType,
        boolean searchable,
        boolean returnable,
        boolean filterable,
        String businessAlias,
        boolean primaryKey
) {
}

