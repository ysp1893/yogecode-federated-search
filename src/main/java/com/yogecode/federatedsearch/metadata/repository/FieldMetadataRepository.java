package com.yogecode.federatedsearch.metadata.repository;

import com.yogecode.federatedsearch.metadata.entity.FieldMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FieldMetadataRepository extends JpaRepository<FieldMetadataEntity, Long> {

    List<FieldMetadataEntity> findByEntityId(Long entityId);
}

