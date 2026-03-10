package com.yogecode.federatedsearch.metadata.model;

public record KeywordMetadataRecord(
        Long id,
        String keyword,
        String entityCode,
        String description,
        boolean active
) {
}

