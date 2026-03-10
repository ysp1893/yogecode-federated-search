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
public class MetadataAdminController {

    private final MetadataService metadataService;

    public MetadataAdminController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/entities")
    public List<EntityMetadataRecord> listEntities() {
        return metadataService.findAllEntities();
    }

    @GetMapping("/entities/{entityId}")
    public EntityMetadataRecord getEntity(@PathVariable("entityId") Long entityId) {
        return metadataService.findEntityById(entityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found"));
    }

    @GetMapping("/entities/{entityId}/fields")
    public List<FieldMetadataRecord> listFields(@PathVariable("entityId") Long entityId) {
        return metadataService.findFieldsByEntityId(entityId);
    }

    @GetMapping("/relations")
    public List<RelationMetadataRecord> listRelations(
            @RequestParam(value = "fromEntityCode", required = false) String fromEntityCode
    ) {
        if (fromEntityCode != null && !fromEntityCode.isBlank()) {
            return metadataService.findRelationsFrom(fromEntityCode);
        }
        return metadataService.findAllRelations();
    }

    @GetMapping("/keywords")
    public List<KeywordMetadataRecord> listKeywords() {
        return metadataService.findAllKeywords();
    }

    @PostMapping("/entities")
    @ResponseStatus(HttpStatus.CREATED)
    public EntityMetadataRecord createEntity(@Valid @RequestBody CreateEntityRequest request) {
        return metadataService.createEntity(request);
    }

    @PostMapping("/entities/{entityId}/fields")
    @ResponseStatus(HttpStatus.CREATED)
    public List<FieldMetadataRecord> createFields(
            @PathVariable("entityId") Long entityId,
            @Valid @RequestBody CreateFieldsRequest request
    ) {
        return metadataService.createFields(entityId, request);
    }

    @PostMapping("/relations")
    @ResponseStatus(HttpStatus.CREATED)
    public RelationMetadataRecord createRelation(@Valid @RequestBody CreateRelationRequest request) {
        return metadataService.createRelation(request);
    }

    @PostMapping("/keywords")
    @ResponseStatus(HttpStatus.CREATED)
    public KeywordMetadataRecord createKeyword(@Valid @RequestBody CreateKeywordRequest request) {
        return metadataService.createKeyword(request);
    }
}
