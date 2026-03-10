package com.yogecode.federatedsearch.cache.model;

import com.yogecode.federatedsearch.datasource.model.RegisteredDataSource;
import com.yogecode.federatedsearch.metadata.model.EntityMetadataRecord;
import com.yogecode.federatedsearch.metadata.model.RelationMetadataRecord;

import java.util.List;
import java.util.Map;

public record SearchMetadataContext(
        String rootEntityCode,
        EntityMetadataRecord rootEntity,
        RegisteredDataSource rootSource,
        List<RelationMetadataRecord> relations,
        Map<String, EntityMetadataRecord> entityByCode,
        Map<Long, RegisteredDataSource> dataSourceById
) {

    public List<RelationMetadataRecord> includedRelations(List<String> include) {
        if (include == null || include.isEmpty()) {
            return List.of();
        }
        return relations.stream()
                .filter(relation -> include.contains(relation.toEntityCode()))
                .toList();
    }

    public EntityMetadataRecord entity(String entityCode) {
        return entityByCode.get(entityCode);
    }

    public RegisteredDataSource dataSource(Long sourceId) {
        return dataSourceById.get(sourceId);
    }
}
