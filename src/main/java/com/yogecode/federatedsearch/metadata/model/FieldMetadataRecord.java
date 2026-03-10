package com.yogecode.federatedsearch.metadata.model;

public record FieldMetadataRecord(
        Long id,
        Long entityId,
        String fieldName,
        String fieldPath,
        String dataType,
        boolean searchable,
        boolean returnable,
        boolean filterable,
        String businessAlias,
        boolean primaryKey,
        boolean active
) {
}

