package com.yogecode.federatedsearch.cache.api;

import com.yogecode.federatedsearch.cache.model.CacheRefreshResponse;
import com.yogecode.federatedsearch.cache.service.CacheAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cache")
@Tag(name = "Cache Admin", description = "Refresh local and Redis-backed cache layers used by metadata and search context resolution.")
public class CacheAdminController {

    private final CacheAdminService cacheAdminService;

    public CacheAdminController(CacheAdminService cacheAdminService) {
        this.cacheAdminService = cacheAdminService;
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh all caches", description = "Clears and repopulates local and Redis cache entries for datasource, metadata, keywords, and search context state.")
    public CacheRefreshResponse refreshAll() {
        return cacheAdminService.refreshAll();
    }

    @PostMapping("/refresh/{scope}")
    @Operation(summary = "Refresh a cache scope", description = "Refreshes a specific cache scope. Use key for a targeted reload such as datasource id, entity code, relation root entity code, keyword, or search context root entity code.")
    public CacheRefreshResponse refreshScope(
            @PathVariable("scope") String scope,
            @RequestParam(value = "key", required = false) String key
    ) {
        return cacheAdminService.refreshScope(scope, key);
    }
}
