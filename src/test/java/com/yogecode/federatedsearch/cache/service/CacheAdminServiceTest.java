package com.yogecode.federatedsearch.cache.service;

import com.yogecode.federatedsearch.cache.model.CacheRefreshResponse;
import com.yogecode.federatedsearch.config.CacheProperties;
import com.yogecode.federatedsearch.metadata.model.EntityMetadataRecord;
import com.yogecode.federatedsearch.metadata.service.MetadataService;
import com.yogecode.federatedsearch.datasource.service.DataSourceService;
import com.yogecode.federatedsearch.common.enums.StorageType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CacheAdminServiceTest {

    private final FederatedCacheManager cacheManager = mock(FederatedCacheManager.class);
    private final SearchMetadataContextService searchMetadataContextService = mock(SearchMetadataContextService.class);
    private final DataSourceService dataSourceService = mock(DataSourceService.class);
    private final MetadataService metadataService = mock(MetadataService.class);
    private final CacheProperties cacheProperties = new CacheProperties();

    private final CacheAdminService service = new CacheAdminService(
            cacheManager,
            searchMetadataContextService,
            dataSourceService,
            metadataService,
            cacheProperties
    );

    @Test
    void refreshEntityReloadsDependentSearchContexts() {
        when(metadataService.findEntityByCode("cdr")).thenReturn(Optional.of(entity("cdr", false)));
        when(cacheManager.isRedisEnabled()).thenReturn(false);

        CacheRefreshResponse response = service.refreshScope("entity", "cdr");

        verify(cacheManager).evictEntityByCode("cdr");
        verify(cacheManager).clearSearchContexts();
        verify(searchMetadataContextService).preloadAllContexts();
        verify(searchMetadataContextService, never()).preloadContext("cdr");
        assertEquals("ENTITY", response.scope());
        assertEquals("cdr", response.key());
    }

    private EntityMetadataRecord entity(String code, boolean rootSearchable) {
        return new EntityMetadataRecord(
                1L,
                code,
                code,
                10L,
                StorageType.TABLE,
                "public",
                code,
                "id",
                code,
                rootSearchable,
                true
        );
    }
}
