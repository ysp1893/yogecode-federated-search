package com.yogecode.federatedsearch.metadata.repository;

import com.yogecode.federatedsearch.metadata.entity.EntityMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntityMetadataRepository extends JpaRepository<EntityMetadataEntity, Long> {

    Optional<EntityMetadataEntity> findByEntityCode(String entityCode);
}

