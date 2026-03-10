package com.yogecode.federatedsearch.metadata.model;

import com.yogecode.federatedsearch.common.enums.JoinStrategy;
import com.yogecode.federatedsearch.common.enums.RelationType;

public record RelationMetadataRecord(
        Long id,
        String relationCode,
        String fromEntityCode,
        String toEntityCode,
        String fromField,
        String toField,
        RelationType relationType,
        JoinStrategy joinStrategy,
        boolean active
) {
}

