package com.yogecode.federatedsearch.cache.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yogecode.federatedsearch.cache.model.SearchMetadataContext;
import com.yogecode.federatedsearch.config.CacheProperties;
import com.yogecode.federatedsearch.datasource.model.RegisteredDataSource;
import com.yogecode.federatedsearch.metadata.model.EntityMetadataRecord;
import com.yogecode.federatedsearch.metadata.model.RelationMetadataRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class FederatedCacheManager {

    private static final Logger log = LoggerFactory.getLogger(FederatedCacheManager.class);

    private final ObjectMapper objectMapper;
    private final Optional<StringRedisTemplate> redisTemplate;
    private final CacheProperties cacheProperties;

    private final Map<Long, RegisteredDataSource> dataSourceCache = new ConcurrentHashMap<>();
    private final Map<String, EntityMetadataRecord> entityByCodeCache = new ConcurrentHashMap<>();
    private final Map<Long, EntityMetadataRecord> entityByIdCache = new ConcurrentHashMap<>();
    private final Map<String, List<RelationMetadataRecord>> relationCache = new ConcurrentHashMap<>();
    private final Map<String, String> keywordCache = new ConcurrentHashMap<>();
    private final Map<String, SearchMetadataContext> searchContextCache = new ConcurrentHashMap<>();

    public FederatedCacheManager(
            ObjectMapper objectMapper,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            CacheProperties cacheProperties
    ) {
        this.objectMapper = objectMapper;
        this.redisTemplate = Optional.ofNullable(redisTemplateProvider.getIfAvailable());
        this.cacheProperties = cacheProperties;
    }

    public boolean isRedisEnabled() {
        return cacheProperties.getRedis().isEnabled() && redisTemplate.isPresent();
    }

    public Optional<RegisteredDataSource> getDataSource(Long sourceId, Supplier<Optional<RegisteredDataSource>> loader) {
        RegisteredDataSource cached = dataSourceCache.get(sourceId);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<RegisteredDataSource> redisValue = readRedisObject(dataSourceKey(sourceId), RegisteredDataSource.class);
        if (redisValue.isPresent()) {
            dataSourceCache.put(sourceId, redisValue.get());
            return redisValue;
        }
        Optional<RegisteredDataSource> loaded = loader.get();
        loaded.ifPresent(this::putDataSource);
        return loaded;
    }

    public void putDataSource(RegisteredDataSource source) {
        dataSourceCache.put(source.id(), source);
        writeRedisObject(dataSourceKey(source.id()), source);
    }

    public void evictDataSource(Long sourceId) {
        dataSourceCache.remove(sourceId);
        deleteRedisKey(dataSourceKey(sourceId));
    }

    public void clearDataSources() {
        dataSourceCache.clear();
        deleteRedisKeysByPrefix("datasource:");
    }

    public Optional<EntityMetadataRecord> getEntityByCode(String entityCode, Supplier<Optional<EntityMetadataRecord>> loader) {
        EntityMetadataRecord cached = entityByCodeCache.get(entityCode);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<EntityMetadataRecord> redisValue = readRedisObject(entityCodeKey(entityCode), EntityMetadataRecord.class);
        if (redisValue.isPresent()) {
            putEntity(redisValue.get());
            return redisValue;
        }
        Optional<EntityMetadataRecord> loaded = loader.get();
        loaded.ifPresent(this::putEntity);
        return loaded;
    }

    public Optional<EntityMetadataRecord> getEntityById(Long entityId, Supplier<Optional<EntityMetadataRecord>> loader) {
        EntityMetadataRecord cached = entityByIdCache.get(entityId);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<EntityMetadataRecord> redisValue = readRedisObject(entityIdKey(entityId), EntityMetadataRecord.class);
        if (redisValue.isPresent()) {
            putEntity(redisValue.get());
            return redisValue;
        }
        Optional<EntityMetadataRecord> loaded = loader.get();
        loaded.ifPresent(this::putEntity);
        return loaded;
    }

    public void putEntity(EntityMetadataRecord entity) {
        entityByCodeCache.put(entity.entityCode(), entity);
        entityByIdCache.put(entity.id(), entity);
        writeRedisObject(entityCodeKey(entity.entityCode()), entity);
        writeRedisObject(entityIdKey(entity.id()), entity);
    }

    public void evictEntityByCode(String entityCode) {
        EntityMetadataRecord cached = entityByCodeCache.remove(entityCode);
        if (cached != null) {
            entityByIdCache.remove(cached.id());
            deleteRedisKey(entityIdKey(cached.id()));
        }
        deleteRedisKey(entityCodeKey(entityCode));
    }

    public void clearEntities() {
        entityByCodeCache.clear();
        entityByIdCache.clear();
        deleteRedisKeysByPrefix("entity:code:");
        deleteRedisKeysByPrefix("entity:id:");
    }

    public Optional<String> getKeyword(String keyword, Supplier<Optional<String>> loader) {
        String cached = keywordCache.get(keyword);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<String> redisValue = readRedisObject(keywordKey(keyword), String.class);
        if (redisValue.isPresent()) {
            keywordCache.put(keyword, redisValue.get());
            return redisValue;
        }
        Optional<String> loaded = loader.get();
        loaded.ifPresent(entityCode -> putKeyword(keyword, entityCode));
        return loaded;
    }

    public void putKeyword(String keyword, String entityCode) {
        keywordCache.put(keyword, entityCode);
        writeRedisObject(keywordKey(keyword), entityCode);
    }

    public void evictKeyword(String keyword) {
        keywordCache.remove(keyword);
        deleteRedisKey(keywordKey(keyword));
    }

    public void clearKeywords() {
        keywordCache.clear();
        deleteRedisKeysByPrefix("keyword:");
    }

    public List<RelationMetadataRecord> getRelations(String entityCode, Supplier<List<RelationMetadataRecord>> loader) {
        List<RelationMetadataRecord> cached = relationCache.get(entityCode);
        if (cached != null) {
            return cached;
        }
        Optional<List<RelationMetadataRecord>> redisValue = readRedisList(relationKey(entityCode));
        if (redisValue.isPresent()) {
            relationCache.put(entityCode, redisValue.get());
            return redisValue.get();
        }
        List<RelationMetadataRecord> loaded = loader.get();
        putRelations(entityCode, loaded);
        return loaded;
    }

    public void putRelations(String entityCode, List<RelationMetadataRecord> relations) {
        relationCache.put(entityCode, relations);
        writeRedisObject(relationKey(entityCode), relations);
    }

    public void evictRelations(String entityCode) {
        relationCache.remove(entityCode);
        deleteRedisKey(relationKey(entityCode));
    }

    public void clearRelations() {
        relationCache.clear();
        deleteRedisKeysByPrefix("relation:");
    }

    public Optional<SearchMetadataContext> getSearchContext(String rootEntityCode, Supplier<Optional<SearchMetadataContext>> loader) {
        SearchMetadataContext cached = searchContextCache.get(rootEntityCode);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<SearchMetadataContext> redisValue = readRedisObject(searchContextKey(rootEntityCode), SearchMetadataContext.class);
        if (redisValue.isPresent()) {
            searchContextCache.put(rootEntityCode, redisValue.get());
            return redisValue;
        }
        Optional<SearchMetadataContext> loaded = loader.get();
        loaded.ifPresent(this::putSearchContext);
        return loaded;
    }

    public void putSearchContext(SearchMetadataContext context) {
        searchContextCache.put(context.rootEntityCode(), context);
        writeRedisObject(searchContextKey(context.rootEntityCode()), context);
    }

    public void evictSearchContext(String rootEntityCode) {
        searchContextCache.remove(rootEntityCode);
        deleteRedisKey(searchContextKey(rootEntityCode));
    }

    public void clearSearchContexts() {
        searchContextCache.clear();
        deleteRedisKeysByPrefix("search-context:");
    }

    public void clearAll() {
        clearDataSources();
        clearEntities();
        clearRelations();
        clearKeywords();
        clearSearchContexts();
    }

    private Optional<List<RelationMetadataRecord>> readRedisList(String key) {
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, RelationMetadataRecord.class);
        return readRedisValue(key, listType);
    }

    private <T> Optional<T> readRedisObject(String key, Class<T> type) {
        return readRedisValue(key, objectMapper.getTypeFactory().constructType(type));
    }

    private <T> Optional<T> readRedisValue(String key, JavaType type) {
        if (!isRedisEnabled()) {
            return Optional.empty();
        }
        try {
            String payload = redisTemplate.get().opsForValue().get(key);
            if (payload == null || payload.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(payload, type));
        } catch (Exception ex) {
            log.warn("Redis read failed for key {}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    private void writeRedisObject(String key, Object value) {
        if (!isRedisEnabled()) {
            return;
        }
        try {
            redisTemplate.get().opsForValue().set(key, objectMapper.writeValueAsString(value));
        } catch (Exception ex) {
            log.warn("Redis write failed for key {}: {}", key, ex.getMessage());
        }
    }

    private void deleteRedisKey(String key) {
        if (!isRedisEnabled()) {
            return;
        }
        try {
            redisTemplate.get().delete(key);
        } catch (Exception ex) {
            log.warn("Redis delete failed for key {}: {}", key, ex.getMessage());
        }
    }

    private void deleteRedisKeysByPrefix(String suffixPrefix) {
        if (!isRedisEnabled()) {
            return;
        }
        try {
            var keys = redisTemplate.get().keys(key(suffixPrefix + "*"));
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.get().delete(keys);
            }
        } catch (Exception ex) {
            log.warn("Redis prefix delete failed for prefix {}: {}", suffixPrefix, ex.getMessage());
        }
    }

    private String dataSourceKey(Long sourceId) {
        return key("datasource:" + sourceId);
    }

    private String entityCodeKey(String entityCode) {
        return key("entity:code:" + entityCode);
    }

    private String entityIdKey(Long entityId) {
        return key("entity:id:" + entityId);
    }

    private String relationKey(String entityCode) {
        return key("relation:" + entityCode);
    }

    private String keywordKey(String keyword) {
        return key("keyword:" + keyword);
    }

    private String searchContextKey(String rootEntityCode) {
        return key("search-context:" + rootEntityCode);
    }

    private String key(String suffix) {
        return cacheProperties.getKeyPrefix() + ":" + suffix;
    }
}
