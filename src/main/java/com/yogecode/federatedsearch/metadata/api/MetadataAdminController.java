package com.yogecode.federatedsearch.metadata.api;

import com.yogecode.federatedsearch.api.metadata.CreateEntityRequest;
import com.yogecode.federatedsearch.api.metadata.CreateFieldsRequest;
import com.yogecode.federatedsearch.api.metadata.CreateKeywordRequest;
import com.yogecode.federatedsearch.api.metadata.CreateRelationRequest;
import com.yogecode.federatedsearch.metadata.model.EntityMetadataRecord;
import com.yogecode.federatedsearch.metadata.model.FieldMetadataRecord;
import com.yogecode.federatedsearch.metadata.model.KeywordMetadataRecord;
import com.yogecode.federatedsearch.metadata.model.RelationMetadataRecord;
import com.yogecode.federatedsearch.metadata.service.MetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Metadata Admin", description = "Manage entity, field, relation, and keyword metadata used by the federated search engine.")
public class MetadataAdminController {

    private final MetadataService metadataService;

    public MetadataAdminController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/entities")
    @Operation(summary = "List entities", description = "Returns all configured business entities mapped to tables or collections.")
    public List<EntityMetadataRecord> listEntities() {
        return metadataService.findAllEntities();
    }

    @GetMapping("/entities/{entityId}")
    @Operation(summary = "Get entity", description = "Returns a single metadata entity by id.")
    public EntityMetadataRecord getEntity(@PathVariable("entityId") Long entityId) {
        return metadataService.findEntityById(entityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found"));
    }

    @GetMapping("/entities/{entityId}/fields")
    @Operation(summary = "List fields for entity", description = "Returns the registered fields for a specific entity.")
    public List<FieldMetadataRecord> listFields(@PathVariable("entityId") Long entityId) {
        return metadataService.findFieldsByEntityId(entityId);
    }

    @GetMapping("/relations")
    @Operation(summary = "List relations", description = "Returns all relations or filters by a source entity using fromEntityCode.")
    public List<RelationMetadataRecord> listRelations(
            @RequestParam(value = "fromEntityCode", required = false) String fromEntityCode
    ) {
        if (fromEntityCode != null && !fromEntityCode.isBlank()) {
            return metadataService.findRelationsFrom(fromEntityCode);
        }
        return metadataService.findAllRelations();
    }

    @GetMapping("/keywords")
    @Operation(summary = "List keywords", description = "Returns all business keywords mapped to entities.")
    public List<KeywordMetadataRecord> listKeywords() {
        return metadataService.findAllKeywords();
    }

    @PostMapping("/entities")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create entity", description = "Registers a metadata entity that maps a business concept to a table or collection.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Customer entity",
                            value = """
                                    {
                                      "entityCode": "customer",
                                      "entityName": "Customer",
                                      "sourceId": 1,
                                      "storageType": "TABLE",
                                      "objectSchema": "public",
                                      "objectName": "customers",
                                      "primaryKeyField": "customer_id",
                                      "businessLabel": "customer",
                                      "rootSearchable": true
                                    }
                                    """
                    )
            )
    )
    public EntityMetadataRecord createEntity(@Valid @org.springframework.web.bind.annotation.RequestBody CreateEntityRequest request) {
        return metadataService.createEntity(request);
    }

    @PostMapping("/entities/{entityId}/fields")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create fields", description = "Registers one or more fields for an existing entity.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Customer fields",
                            value = """
                                    {
                                      "fields": [
                                        {
                                          "fieldName": "customer_id",
                                          "dataType": "LONG",
                                          "searchable": false,
                                          "returnable": true,
                                          "filterable": true,
                                          "businessAlias": "customerId",
                                          "primaryKey": true
                                        },
                                        {
                                          "fieldName": "name",
                                          "dataType": "STRING",
                                          "searchable": true,
                                          "returnable": true,
                                          "filterable": true,
                                          "businessAlias": "name",
                                          "primaryKey": false
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    public List<FieldMetadataRecord> createFields(
            @PathVariable("entityId") Long entityId,
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateFieldsRequest request
    ) {
        return metadataService.createFields(entityId, request);
    }

    @PostMapping("/relations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create relation", description = "Registers a cross-entity relation used by the search planner.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Customer to orders",
                            value = """
                                    {
                                      "relationCode": "customer_orders",
                                      "fromEntityCode": "customer",
                                      "toEntityCode": "orders",
                                      "fromField": "customer_id",
                                      "toField": "customer_id",
                                      "relationType": "ONE_TO_MANY",
                                      "joinStrategy": "IN"
                                    }
                                    """
                    )
            )
    )
    public RelationMetadataRecord createRelation(@Valid @org.springframework.web.bind.annotation.RequestBody CreateRelationRequest request) {
        return metadataService.createRelation(request);
    }

    @PostMapping("/keywords")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create keyword", description = "Binds a business keyword like user or order to a registered entity.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Keyword binding",
                            value = """
                                    {
                                      "keyword": "user",
                                      "entityCode": "customer",
                                      "description": "User maps to customer"
                                    }
                                    """
                    )
            )
    )
    public KeywordMetadataRecord createKeyword(@Valid @org.springframework.web.bind.annotation.RequestBody CreateKeywordRequest request) {
        return metadataService.createKeyword(request);
    }
}
