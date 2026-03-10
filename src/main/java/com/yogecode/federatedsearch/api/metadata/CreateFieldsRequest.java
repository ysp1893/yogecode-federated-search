package com.yogecode.federatedsearch.api.metadata;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateFieldsRequest(
        @Valid @NotEmpty List<FieldRequest> fields
) {
}

