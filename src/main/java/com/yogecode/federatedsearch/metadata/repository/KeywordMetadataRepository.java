package com.yogecode.federatedsearch.metadata.repository;

import com.yogecode.federatedsearch.metadata.entity.KeywordMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KeywordMetadataRepository extends JpaRepository<KeywordMetadataEntity, Long> {

    Optional<KeywordMetadataEntity> findByKeyword(String keyword);
}

