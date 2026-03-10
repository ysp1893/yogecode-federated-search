package com.yogecode.federatedsearch.search.service;

import com.yogecode.federatedsearch.api.search.SearchRequest;
import com.yogecode.federatedsearch.api.search.SearchResponse;

public interface FederatedSearchService {

    SearchResponse search(SearchRequest request);
}

