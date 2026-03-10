package com.yogecode.federatedsearch.metadata.service;

import com.yogecode.federatedsearch.api.metadata.CreateEntityRequest;
import com.yogecode.federatedsearch.api.metadata.CreateFieldsRequest;
import com.yogecode.federatedsearch.api.metadata.CreateKeywordRequest;
import com.yogecode.federatedsearch.api.metadata.CreateRelationRequest;
import com.yogecode.federatedsearch.cache.service.FederatedCacheManager;
import com.yogecode.federatedsearch.datasource.entity.DataSourceEntity;
import com.yogecode.federatedsearch.datasource.repository.DataSourceRepository;
import com.yogecode.federatedsearch.metadata.entity.EntityMetadataEntity;
import com.yogecode.federatedsearch.metadata.entity.FieldMetadataEntity;
import com.yogecode.federatedsearch.metadata.entity.KeywordMetadataEntity;
import com.yogecode.federatedsearch.metadata.entity.RelationMetadataEntity;
import com.yogecode.federatedsearch.metadata.model.EntityMetadataRecord;
import com.yogecode.federatedsearch.metadata.model.FieldMetadataRecord;
import com.yogecode.federatedsearch.metadata.model.KeywordMetadataRecord;
import com.yogecode.federatedsearch.metadata.model.RelationMetadataRecord;
import com.yogecode.federatedsearch.metadata.repository.EntityMetadataRepository;
import com.yogecode.federatedsearch.metadata.repository.FieldMetadataRepository;
import com.yogecode.federatedsearch.metadata.repository.KeywordMetadataRepository;
import com.yogecode.federatedsearch.metadata.repository.RelationMetadataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class CachedMetadataService implements MetadataService {

    private final EntityMetadataRepository entityMetadataRepository;
    private final FieldMetadataRepository fieldMetadataRepository;
    private final RelationMetadataRepository relationMetadataRepository;
    private final KeywordMetadataRepository keywordMetadataRepository;
    private final DataSourceRepository dataSourceRepository;
    private final FederatedCacheManager cacheManager;

    public CachedMetadataService(
            EntityMetadataRepository entityMetadataRepository,
            FieldMetadataRepository fieldMetadataRepository,
            RelationMetadataRepository relationMetadataRepository,
            KeywordMetadataRepository keywordMetadataRepository,
            DataSourceRepository dataSourceRepository,
            FederatedCacheManager cacheManager
    ) {
        this.entityMetadataRepository = entityMetadataRepository;
        this.fieldMetadataRepository = fieldMetadataRepository;
        this.relationMetadataRepository = relationMetadataRepository;
        this.keywordMetadataRepository = keywordMetadataRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    public EntityMetadataRecord createEntity(CreateEntityRequest request) {
        DataSourceEntity source = dataSourceRepository.findById(request.sourceId())
                .orElseThrow(() -> new IllegalArgumentException("Datasource not found: " + request.sourceId()));

        EntityMetadataEntity entity = new EntityMetadataEntity();
        entity.setEntityCode(request.entityCode());
        entity.setEntityName(request.entityName());
        entity.setSource(source);
        entity.setStorageType(request.storageType());
        entity.setObjectSchema(request.objectSchema());
        entity.setObjectName(request.objectName());
        entity.setPrimaryKeyField(request.primaryKeyField());
        entity.setBusinessLabel(request.businessLabel());
        entity.setRootSearchable(request.rootSearchable());
        entity.setActive(true);

        EntityMetadataRecord record = toEntityRecord(entityMetadataRepository.save(entity));
        cacheManager.putEntity(record);
        cacheManager.clearSearchContexts();
        return record;
    }

    @Override
    public List<FieldMetadataRecord> createFields(Long entityId, CreateFieldsRequest request) {
        EntityMetadataEntity entity = entityMetadataRepository.findById(entityId)
                .orElseThrow(() -> new IllegalArgumentException("Entity not found: " + entityId));

        List<FieldMetadataEntity> entities = request.fields().stream().map(field -> {
            FieldMetadataEntity fieldEntity = new FieldMetadataEntity();
            fieldEntity.setEntity(entity);
            fieldEntity.setFieldName(field.fieldName());
            fieldEntity.setFieldPath(field.fieldPath());
            fieldEntity.setDataType(field.dataType());
            fieldEntity.setSearchable(field.searchable());
            fieldEntity.setReturnable(field.returnable());
            fieldEntity.setFilterable(field.filterable());
            fieldEntity.setBusinessAlias(field.businessAlias());
            fieldEntity.setPrimaryKey(field.primaryKey());
            fieldEntity.setActive(true);
            return fieldEntity;
        }).toList();

        return fieldMetadataRepository.saveAll(entities).stream()
                .map(this::toFieldRecord)
                .toList();
    }

    @Override
    public RelationMetadataRecord createRelation(CreateRelationRequest request) {
        EntityMetadataEntity fromEntity = entityMetadataRepository.findByEntityCode(request.fromEntityCode())
                .orElseThrow(() -> new IllegalArgumentException("Unknown fromEntityCode: " + request.fromEntityCode()));
        EntityMetadataEntity toEntity = entityMetadataRepository.findByEntityCode(request.toEntityCode())
                .orElseThrow(() -> new IllegalArgumentException("Unknown toEntityCode: " + request.toEntityCode()));

        RelationMetadataEntity relation = new RelationMetadataEntity();
        relation.setRelationCode(request.relationCode());
        relation.setFromEntity(fromEntity);
        relation.setToEntity(toEntity);
        relation.setFromField(request.fromField());
        relation.setToField(request.toField());
        relation.setRelationType(request.relationType());
        relation.setJoinStrategy(request.joinStrategy());
        relation.setActive(true);

        RelationMetadataRecord record = toRelationRecord(relationMetadataRepository.save(relation));
        cacheManager.evictRelations(request.fromEntityCode());
        cacheManager.evictSearchContext(request.fromEntityCode());
        return record;
    }

    @Override
    public KeywordMetadataRecord createKeyword(CreateKeywordRequest request) {
        String normalizedKeyword = request.keyword().toLowerCase(Locale.ROOT);
        EntityMetadataEntity entity = entityMetadataRepository.findByEntityCode(request.entityCode())
                .orElseThrow(() -> new IllegalArgumentException("Unknown entityCode: " + request.entityCode()));

        KeywordMetadataEntity keyword = new KeywordMetadataEntity();
        keyword.setKeyword(normalizedKeyword);
        keyword.setEntity(entity);
        keyword.setDescription(request.description());
        keyword.setActive(true);

        KeywordMetadataRecord record = toKeywordRecord(keywordMetadataRepository.save(keyword));
        cacheManager.putKeyword(record.keyword(), record.entityCode());
        return record;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EntityMetadataRecord> findEntityByCode(String entityCode) {
        return cacheManager.getEntityByCode(entityCode, () -> entityMetadataRepository.findByEntityCode(entityCode).map(this::toEntityRecord));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EntityMetadataRecord> findEntityById(Long entityId) {
        return cacheManager.getEntityById(entityId, () -> entityMetadataRepository.findById(entityId).map(this::toEntityRecord));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityMetadataRecord> findAllEntities() {
        List<EntityMetadataRecord> records = entityMetadataRepository.findAll().stream()
                .map(this::toEntityRecord)
                .toList();
        records.forEach(cacheManager::putEntity);
        return records;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldMetadataRecord> findFieldsByEntityId(Long entityId) {
        return fieldMetadataRepository.findByEntityId(entityId).stream()
                .map(this::toFieldRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> resolveEntityByKeyword(String keyword) {
        if (keyword == null) {
            return Optional.empty();
        }
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        return cacheManager.getKeyword(normalizedKeyword, () -> keywordMetadataRepository.findByKeyword(normalizedKeyword)
                .map(this::toKeywordRecord)
                .map(KeywordMetadataRecord::entityCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RelationMetadataRecord> findRelationsFrom(String entityCode) {
        return cacheManager.getRelations(entityCode, () -> relationMetadataRepository.findByFromEntityEntityCode(entityCode).stream()
                .map(this::toRelationRecord)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RelationMetadataRecord> findAllRelations() {
        List<RelationMetadataRecord> relations = relationMetadataRepository.findAll().stream()
                .map(this::toRelationRecord)
                .toList();
        relations.stream()
                .map(RelationMetadataRecord::fromEntityCode)
                .distinct()
                .forEach(entityCode -> cacheManager.putRelations(entityCode,
                        relations.stream().filter(relation -> relation.fromEntityCode().equals(entityCode)).toList()));
        return relations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KeywordMetadataRecord> findAllKeywords() {
        List<KeywordMetadataRecord> keywords = keywordMetadataRepository.findAll().stream()
                .map(this::toKeywordRecord)
                .toList();
        keywords.forEach(keyword -> cacheManager.putKeyword(keyword.keyword(), keyword.entityCode()));
        return keywords;
    }

    private EntityMetadataRecord toEntityRecord(EntityMetadataEntity entity) {
        return new EntityMetadataRecord(
                entity.getId(),
                entity.getEntityCode(),
                entity.getEntityName(),
                entity.getSource().getId(),
                entity.getStorageType(),
                entity.getObjectSchema(),
                entity.getObjectName(),
                entity.getPrimaryKeyField(),
                entity.getBusinessLabel(),
                entity.isRootSearchable(),
                entity.isActive()
        );
    }

    private FieldMetadataRecord toFieldRecord(FieldMetadataEntity entity) {
        return new FieldMetadataRecord(
                entity.getId(),
                entity.getEntity().getId(),
                entity.getFieldName(),
                entity.getFieldPath(),
                entity.getDataType(),
                entity.isSearchable(),
                entity.isReturnable(),
                entity.isFilterable(),
                entity.getBusinessAlias(),
                entity.isPrimaryKey(),
                entity.isActive()
        );
    }

    private RelationMetadataRecord toRelationRecord(RelationMetadataEntity entity) {
        return new RelationMetadataRecord(
                entity.getId(),
                entity.getRelationCode(),
                entity.getFromEntity().getEntityCode(),
                entity.getToEntity().getEntityCode(),
                entity.getFromField(),
                entity.getToField(),
                entity.getRelationType(),
                entity.getJoinStrategy(),
                entity.isActive()
        );
    }

    private KeywordMetadataRecord toKeywordRecord(KeywordMetadataEntity entity) {
        return new KeywordMetadataRecord(
                entity.getId(),
                entity.getKeyword(),
                entity.getEntity().getEntityCode(),
                entity.getDescription(),
                entity.isActive()
        );
    }
}
