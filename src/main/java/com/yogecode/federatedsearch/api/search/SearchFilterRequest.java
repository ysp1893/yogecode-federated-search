package com.yogecode.federatedsearch.api.search;

import com.yogecode.federatedsearch.common.enums.FilterOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SearchFilterRequest(
        @NotBlank String field,
        @NotNull FilterOperator operator,
        Object value
) {
}
