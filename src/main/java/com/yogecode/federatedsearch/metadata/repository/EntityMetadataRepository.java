package com.yogecode.federatedsearch.metadata.repository;

import com.yogecode.federatedsearch.metadata.entity.EntityMetadataEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntityMetadataRepository extends JpaRepository<EntityMetadataEntity, Long> {

    @EntityGraph(attributePaths = "source")
    Optional<EntityMetadataEntity> findByEntityCode(String entityCode);

    @Override
    @EntityGraph(attributePaths = "source")
    Optional<EntityMetadataEntity> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "source")
    List<EntityMetadataEntity> findAll();
}
