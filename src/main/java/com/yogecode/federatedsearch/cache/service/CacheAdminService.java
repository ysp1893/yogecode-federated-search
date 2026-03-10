package com.yogecode.federatedsearch.cache.service;

import com.yogecode.federatedsearch.cache.model.CacheRefreshResponse;
import com.yogecode.federatedsearch.cache.model.CacheScope;
import com.yogecode.federatedsearch.config.CacheProperties;
import com.yogecode.federatedsearch.datasource.service.DataSourceService;
import com.yogecode.federatedsearch.metadata.service.MetadataService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;

@Service
public class CacheAdminService {

    private final FederatedCacheManager cacheManager;
    private final SearchMetadataContextService searchMetadataContextService;
    private final DataSourceService dataSourceService;
    private final MetadataService metadataService;
    private final CacheProperties cacheProperties;

    public CacheAdminService(
            FederatedCacheManager cacheManager,
            SearchMetadataContextService searchMetadataContextService,
            DataSourceService dataSourceService,
            MetadataService metadataService,
            CacheProperties cacheProperties
    ) {
        this.cacheManager = cacheManager;
        this.searchMetadataContextService = searchMetadataContextService;
        this.dataSourceService = dataSourceService;
        this.metadataService = metadataService;
        this.cacheProperties = cacheProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void preloadOnStartup() {
        if (cacheProperties.isPreloadOnStartup()) {
            refreshAll();
        }
    }

    public CacheRefreshResponse refreshAll() {
        cacheManager.clearAll();
        warmAllBaseCaches();
        searchMetadataContextService.preloadAllContexts();
        return response(CacheScope.ALL, null, "Reloaded all local and Redis cache entries.");
    }

    public CacheRefreshResponse refreshScope(String scopeValue, String key) {
        CacheScope scope = parseScope(scopeValue);
        return switch (scope) {
            case ALL -> refreshAll();
            case METADATA -> refreshMetadata();
            case DATASOURCE -> refreshDatasource(key);
            case ENTITY -> refreshEntity(key);
            case RELATION -> refreshRelation(key);
            case KEYWORD -> refreshKeyword(key);
            case SEARCH_CONTEXT -> refreshSearchContext(key);
        };
    }

    private CacheRefreshResponse refreshMetadata() {
        cacheManager.clearEntities();
        cacheManager.clearRelations();
        cacheManager.clearKeywords();
        cacheManager.clearSearchContexts();
        metadataService.findAllEntities();
        metadataService.findAllRelations();
        metadataService.findAllKeywords();
        searchMetadataContextService.preloadAllContexts();
        return response(CacheScope.METADATA, null, "Reloaded metadata and search context caches.");
    }

    private CacheRefreshResponse refreshDatasource(String key) {
        if (key == null || key.isBlank()) {
            cacheManager.clearDataSources();
            cacheManager.clearSearchContexts();
            dataSourceService.findAll();
            searchMetadataContextService.preloadAllContexts();
            return response(CacheScope.DATASOURCE, null, "Reloaded all datasource cache entries.");
        }
        Long sourceId = parseLongKey(key, "datasource id");
        cacheManager.evictDataSource(sourceId);
        cacheManager.clearSearchContexts();
        dataSourceService.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Datasource not found: " + sourceId));
        searchMetadataContextService.preloadAllContexts();
        return response(CacheScope.DATASOURCE, key, "Reloaded datasource cache entry and dependent search contexts.");
    }

    private CacheRefreshResponse refreshEntity(String key) {
        if (key == null || key.isBlank()) {
            cacheManager.clearEntities();
            cacheManager.clearSearchContexts();
            metadataService.findAllEntities();
            searchMetadataContextService.preloadAllContexts();
            return response(CacheScope.ENTITY, null, "Reloaded all entity cache entries.");
        }
        cacheManager.evictEntityByCode(key);
        cacheManager.evictSearchContext(key);
        metadataService.findEntityByCode(key)
                .orElseThrow(() -> new IllegalArgumentException("Entity not found: " + key));
        searchMetadataContextService.preloadContext(key);
        return response(CacheScope.ENTITY, key, "Reloaded entity cache entry.");
    }

    private CacheRefreshResponse refreshRelation(String key) {
        if (key == null || key.isBlank()) {
            cacheManager.clearRelations();
            cacheManager.clearSearchContexts();
            metadataService.findAllRelations();
            searchMetadataContextService.preloadAllContexts();
            return response(CacheScope.RELATION, null, "Reloaded all relation cache entries.");
        }
        cacheManager.evictRelations(key);
        cacheManager.evictSearchContext(key);
        metadataService.findRelationsFrom(key);
        searchMetadataContextService.preloadContext(key);
        return response(CacheScope.RELATION, key, "Reloaded relation cache entry for root entity.");
    }

    private CacheRefreshResponse refreshKeyword(String key) {
        if (key == null || key.isBlank()) {
            cacheManager.clearKeywords();
            metadataService.findAllKeywords();
            return response(CacheScope.KEYWORD, null, "Reloaded all keyword cache entries.");
        }
        cacheManager.evictKeyword(key.toLowerCase(Locale.ROOT));
        metadataService.resolveEntityByKeyword(key)
                .orElseThrow(() -> new IllegalArgumentException("Keyword not found: " + key));
        return response(CacheScope.KEYWORD, key, "Reloaded keyword cache entry.");
    }

    private CacheRefreshResponse refreshSearchContext(String key) {
        if (key == null || key.isBlank()) {
            cacheManager.clearSearchContexts();
            searchMetadataContextService.preloadAllContexts();
            return response(CacheScope.SEARCH_CONTEXT, null, "Reloaded all search context cache entries.");
        }
        cacheManager.evictSearchContext(key);
        searchMetadataContextService.preloadContext(key);
        return response(CacheScope.SEARCH_CONTEXT, key, "Reloaded search context cache entry.");
    }

    private void warmAllBaseCaches() {
        dataSourceService.findAll();
        metadataService.findAllEntities();
        metadataService.findAllRelations();
        metadataService.findAllKeywords();
    }

    private CacheScope parseScope(String scopeValue) {
        try {
            return CacheScope.valueOf(scopeValue.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported cache scope: " + scopeValue);
        }
    }

    private Long parseLongKey(String key, String label) {
        try {
            return Long.parseLong(key);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + label + ": " + key);
        }
    }

    private CacheRefreshResponse response(CacheScope scope, String key, String message) {
        return new CacheRefreshResponse(
                Instant.now(),
                scope.name(),
                key,
                "SUCCESS",
                message,
                cacheManager.isRedisEnabled()
        );
    }
}
