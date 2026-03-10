package com.yogecode.federatedsearch.metadata.repository;

import com.yogecode.federatedsearch.metadata.entity.RelationMetadataEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelationMetadataRepository extends JpaRepository<RelationMetadataEntity, Long> {

    @EntityGraph(attributePaths = {"fromEntity", "toEntity"})
    List<RelationMetadataEntity> findByFromEntityEntityCode(String entityCode);

    @Override
    @EntityGraph(attributePaths = {"fromEntity", "toEntity"})
    List<RelationMetadataEntity> findAll();
}
