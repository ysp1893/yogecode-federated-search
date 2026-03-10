package com.yogecode.federatedsearch.metadata.repository;

import com.yogecode.federatedsearch.metadata.entity.KeywordMetadataEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KeywordMetadataRepository extends JpaRepository<KeywordMetadataEntity, Long> {

    @EntityGraph(attributePaths = "entity")
    Optional<KeywordMetadataEntity> findByKeyword(String keyword);

    @Override
    @EntityGraph(attributePaths = "entity")
    List<KeywordMetadataEntity> findAll();
}
