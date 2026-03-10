package com.yogecode.federatedsearch.cache.service;

import com.yogecode.federatedsearch.cache.model.SearchMetadataContext;
import com.yogecode.federatedsearch.datasource.model.RegisteredDataSource;
import com.yogecode.federatedsearch.datasource.service.DataSourceService;
import com.yogecode.federatedsearch.metadata.model.EntityMetadataRecord;
import com.yogecode.federatedsearch.metadata.model.RelationMetadataRecord;
import com.yogecode.federatedsearch.metadata.service.MetadataService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SearchMetadataContextService {

    private final FederatedCacheManager cacheManager;
    private final MetadataService metadataService;
    private final DataSourceService dataSourceService;

    public SearchMetadataContextService(
            FederatedCacheManager cacheManager,
            MetadataService metadataService,
            DataSourceService dataSourceService
    ) {
        this.cacheManager = cacheManager;
        this.metadataService = metadataService;
        this.dataSourceService = dataSourceService;
    }

    public SearchMetadataContext getContext(String rootEntityCode) {
        return cacheManager.getSearchContext(rootEntityCode, () -> loadContext(rootEntityCode))
                .orElseThrow(() -> new IllegalArgumentException("Unknown root entity: " + rootEntityCode));
    }

    public void preloadAllContexts() {
        metadataService.findAllEntities().stream()
                .filter(EntityMetadataRecord::rootSearchable)
                .forEach(entity -> cacheManager.putSearchContext(buildContext(entity.entityCode())));
    }

    public void preloadContext(String rootEntityCode) {
        cacheManager.putSearchContext(buildContext(rootEntityCode));
    }

    public void evictContext(String rootEntityCode) {
        cacheManager.evictSearchContext(rootEntityCode);
    }

    public void evictAll() {
        cacheManager.clearSearchContexts();
    }

    private Optional<SearchMetadataContext> loadContext(String rootEntityCode) {
        return metadataService.findEntityByCode(rootEntityCode).map(root -> buildContext(root.entityCode()));
    }

    private SearchMetadataContext buildContext(String rootEntityCode) {
        EntityMetadataRecord rootEntity = metadataService.findEntityByCode(rootEntityCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown root entity: " + rootEntityCode));
        RegisteredDataSource rootSource = dataSourceService.findById(rootEntity.sourceId())
                .orElseThrow(() -> new IllegalArgumentException("Datasource not found for root entity: " + rootEntityCode));
        List<RelationMetadataRecord> relations = metadataService.findRelationsFrom(rootEntityCode);

        Map<String, EntityMetadataRecord> entityByCode = new LinkedHashMap<>();
        entityByCode.put(rootEntity.entityCode(), rootEntity);

        Map<Long, RegisteredDataSource> dataSourceById = new LinkedHashMap<>();
        dataSourceById.put(rootSource.id(), rootSource);

        for (RelationMetadataRecord relation : relations) {
            EntityMetadataRecord relatedEntity = metadataService.findEntityByCode(relation.toEntityCode())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown related entity: " + relation.toEntityCode()));
            entityByCode.put(relatedEntity.entityCode(), relatedEntity);
            dataSourceById.computeIfAbsent(relatedEntity.sourceId(), sourceId -> dataSourceService.findById(sourceId)
                    .orElseThrow(() -> new IllegalArgumentException("Datasource not found for related entity: " + relation.toEntityCode())));
        }

        return new SearchMetadataContext(
                rootEntityCode,
                rootEntity,
                rootSource,
                relations,
                entityByCode,
                dataSourceById
        );
    }
}
