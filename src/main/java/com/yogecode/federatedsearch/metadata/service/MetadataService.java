package com.yogecode.federatedsearch.metadata.service;

import com.yogecode.federatedsearch.api.metadata.CreateEntityRequest;
import com.yogecode.federatedsearch.api.metadata.CreateFieldsRequest;
import com.yogecode.federatedsearch.api.metadata.CreateKeywordRequest;
import com.yogecode.federatedsearch.api.metadata.CreateRelationRequest;
import com.yogecode.federatedsearch.metadata.model.EntityMetadataRecord;
import com.yogecode.federatedsearch.metadata.model.FieldMetadataRecord;
import com.yogecode.federatedsearch.metadata.model.KeywordMetadataRecord;
import com.yogecode.federatedsearch.metadata.model.RelationMetadataRecord;

import java.util.List;
import java.util.Optional;

public interface MetadataService {

    EntityMetadataRecord createEntity(CreateEntityRequest request);

    List<FieldMetadataRecord> createFields(Long entityId, CreateFieldsRequest request);

    RelationMetadataRecord createRelation(CreateRelationRequest request);

    KeywordMetadataRecord createKeyword(CreateKeywordRequest request);

    Optional<EntityMetadataRecord> findEntityByCode(String entityCode);

    Optional<EntityMetadataRecord> findEntityById(Long entityId);

    List<EntityMetadataRecord> findAllEntities();

    List<FieldMetadataRecord> findFieldsByEntityId(Long entityId);

    Optional<String> resolveEntityByKeyword(String keyword);

    List<RelationMetadataRecord> findRelationsFrom(String entityCode);

    List<RelationMetadataRecord> findAllRelations();

    List<KeywordMetadataRecord> findAllKeywords();
}
