package com.yogecode.federatedsearch.search.api;

import com.yogecode.federatedsearch.api.search.SearchRequest;
import com.yogecode.federatedsearch.api.search.SearchResponse;
import com.yogecode.federatedsearch.search.service.FederatedSearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final FederatedSearchService federatedSearchService;

    public SearchController(FederatedSearchService federatedSearchService) {
        this.federatedSearchService = federatedSearchService;
    }

    @PostMapping
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        return federatedSearchService.search(request);
    }
}

