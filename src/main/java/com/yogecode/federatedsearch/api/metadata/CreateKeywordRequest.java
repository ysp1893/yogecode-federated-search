package com.yogecode.federatedsearch.api.metadata;

import jakarta.validation.constraints.NotBlank;

public record CreateKeywordRequest(
        @NotBlank String keyword,
        @NotBlank String entityCode,
        String description
) {
}

