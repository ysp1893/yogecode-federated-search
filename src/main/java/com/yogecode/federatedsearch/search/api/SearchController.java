package com.yogecode.federatedsearch.search.api;

import com.yogecode.federatedsearch.api.search.SearchRequest;
import com.yogecode.federatedsearch.api.search.SearchResponse;
import com.yogecode.federatedsearch.search.service.FederatedSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Execute structured federated search requests across configured business entities.")
public class SearchController {

    private final FederatedSearchService federatedSearchService;

    public SearchController(FederatedSearchService federatedSearchService) {
        this.federatedSearchService = federatedSearchService;
    }

    @PostMapping
    @Operation(summary = "Execute federated search", description = "Accepts a structured search request and resolves it against configured metadata, datasource mappings, and connectors.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "Entity-based search",
                                    value = """
                                            {
                                              "entity": "customer",
                                              "filters": [
                                                {
                                                  "field": "name",
                                                  "operator": "EQ",
                                                  "value": "yogesh"
                                                }
                                              ],
                                              "fields": ["custid", "username", "name"],
                                              "include": ["cdr", "tickets"],
                                              "sortBy": "custid",
                                              "sortDirection": "DESC",
                                              "page": 0,
                                              "size": 20
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Entity fields search",
                                    value = """
                                            {
                                              "entity": "cdr",
                                              "entityFields": {
                                                "cdr": ["CDRID", "UserName", "AcctStatusType", "lastmodificationdate"],
                                                "customer": ["aadhar"]
                                              },
                                              "include": ["customer"],
                                              "page": 0,
                                              "size": 20
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Relation null filter",
                                    value = """
                                            {
                                              "entity": "customer",
                                              "filters": [
                                                {
                                                  "field": "cdr.CDRID",
                                                  "operator": "IS_NULL"
                                                }
                                              ],
                                              "entityFields": {
                                                "customer": ["username", "cstatus"],
                                                "cdr": ["CDRID"]
                                              },
                                              "include": ["cdr"],
                                              "page": 0,
                                              "size": 20
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Keyword-based search",
                                    value = """
                                            {
                                              "keyword": "user",
                                              "filters": [
                                                {
                                                  "field": "name",
                                                  "operator": "EQ",
                                                  "value": "yogesh"
                                                }
                                              ],
                                              "include": ["orders", "invoice"]
                                            }
                                            """
                            )
                    }
            )
    )
    public SearchResponse search(@Valid @org.springframework.web.bind.annotation.RequestBody SearchRequest request) {
        return federatedSearchService.search(request);
    }
}
