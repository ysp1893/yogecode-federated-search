package com.yogecode.federatedsearch.datasource.service;

import com.yogecode.federatedsearch.api.datasource.ConnectionTestResponse;
import com.yogecode.federatedsearch.api.datasource.CreateDataSourceRequest;
import com.yogecode.federatedsearch.api.datasource.DataSourceDetailsResponse;
import com.yogecode.federatedsearch.api.datasource.DataSourceResponse;
import com.yogecode.federatedsearch.datasource.model.RegisteredDataSource;

import java.util.List;
import java.util.Optional;

public interface DataSourceService {

    DataSourceResponse create(CreateDataSourceRequest request);

    ConnectionTestResponse testConnection(Long sourceId);

    Optional<RegisteredDataSource> findById(Long sourceId);

    List<RegisteredDataSource> findAll();

    Optional<DataSourceDetailsResponse> getDetails(Long sourceId);

    List<DataSourceDetailsResponse> getAllDetails();
}
