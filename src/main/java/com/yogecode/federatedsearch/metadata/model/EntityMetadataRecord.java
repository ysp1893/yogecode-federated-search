package com.yogecode.federatedsearch.metadata.model;

import com.yogecode.federatedsearch.common.enums.StorageType;

public record EntityMetadataRecord(
        Long id,
        String entityCode,
        String entityName,
        Long sourceId,
        StorageType storageType,
        String objectSchema,
        String objectName,
        String primaryKeyField,
        String businessLabel,
        boolean rootSearchable,
        boolean active
) {
}

