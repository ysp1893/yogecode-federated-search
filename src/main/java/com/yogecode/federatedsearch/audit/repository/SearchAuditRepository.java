package com.yogecode.federatedsearch.audit.repository;

import com.yogecode.federatedsearch.audit.entity.SearchAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchAuditRepository extends JpaRepository<SearchAuditEntity, Long> {
}
