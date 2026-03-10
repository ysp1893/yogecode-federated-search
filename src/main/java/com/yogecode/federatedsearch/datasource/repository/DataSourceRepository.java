package com.yogecode.federatedsearch.datasource.repository;

import com.yogecode.federatedsearch.datasource.entity.DataSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSourceRepository extends JpaRepository<DataSourceEntity, Long> {
}

