package com.yogecode.federatedsearch.api.metadata;

import com.yogecode.federatedsearch.common.enums.JoinStrategy;
import com.yogecode.federatedsearch.common.enums.RelationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRelationRequest(
        @NotBlank String relationCode,
        @NotBlank String fromEntityCode,
        @NotBlank String toEntityCode,
        @NotBlank String fromField,
        @NotBlank String toField,
        @NotNull RelationType relationType,
        @NotNull JoinStrategy joinStrategy
) {
}

