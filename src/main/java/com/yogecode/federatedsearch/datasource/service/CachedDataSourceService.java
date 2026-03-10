package com.yogecode.federatedsearch.datasource.service;

import com.yogecode.federatedsearch.api.datasource.ConnectionTestResponse;
import com.yogecode.federatedsearch.api.datasource.CreateDataSourceRequest;
import com.yogecode.federatedsearch.api.datasource.DataSourceDetailsResponse;
import com.yogecode.federatedsearch.api.datasource.DataSourceResponse;
import com.yogecode.federatedsearch.cache.service.FederatedCacheManager;
import com.yogecode.federatedsearch.datasource.entity.DataSourceEntity;
import com.yogecode.federatedsearch.datasource.model.RegisteredDataSource;
import com.yogecode.federatedsearch.datasource.repository.DataSourceRepository;
import com.yogecode.federatedsearch.security.CredentialCipher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CachedDataSourceService implements DataSourceService {

    private final DataSourceRepository dataSourceRepository;
    private final CredentialCipher credentialCipher;
    private final FederatedCacheManager cacheManager;

    public CachedDataSourceService(
            DataSourceRepository dataSourceRepository,
            CredentialCipher credentialCipher,
            FederatedCacheManager cacheManager
    ) {
        this.dataSourceRepository = dataSourceRepository;
        this.credentialCipher = credentialCipher;
        this.cacheManager = cacheManager;
    }

    @Override
    public DataSourceResponse create(CreateDataSourceRequest request) {
        DataSourceEntity entity = new DataSourceEntity();
        entity.setSourceCode(request.sourceCode());
        entity.setSourceName(request.sourceName());
        entity.setDbType(request.dbType());
        entity.setHost(request.host());
        entity.setPort(request.port());
        entity.setDatabaseName(request.databaseName());
        entity.setUsername(request.username());
        entity.setPasswordRef(credentialCipher.encrypt(request.password()));
        entity.setConnectionParams(request.connectionParams());
        entity.setActive(true);

        DataSourceEntity saved = dataSourceRepository.save(entity);
        cacheManager.putDataSource(toRegisteredDataSource(saved));
        cacheManager.clearSearchContexts();
        return new DataSourceResponse(saved.getId(), saved.getSourceCode(), saved.getSourceName(), saved.getDbType(), "CREATED");
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectionTestResponse testConnection(Long sourceId) {
        RegisteredDataSource source = findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Datasource not found: " + sourceId));
        return new ConnectionTestResponse(source.id(), "SUCCESS", "Metadata stored. Connector ping can be added next.");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RegisteredDataSource> findById(Long sourceId) {
        return cacheManager.getDataSource(sourceId, () -> dataSourceRepository.findById(sourceId).map(this::toRegisteredDataSource));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegisteredDataSource> findAll() {
        List<RegisteredDataSource> records = dataSourceRepository.findAll().stream()
                .map(this::toRegisteredDataSource)
                .toList();
        records.forEach(cacheManager::putDataSource);
        return records;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DataSourceDetailsResponse> getDetails(Long sourceId) {
        return findById(sourceId).map(this::toDetailsResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataSourceDetailsResponse> getAllDetails() {
        return findAll().stream()
                .map(this::toDetailsResponse)
                .toList();
    }

    private RegisteredDataSource toRegisteredDataSource(DataSourceEntity entity) {
        return new RegisteredDataSource(
                entity.getId(),
                entity.getSourceCode(),
                entity.getSourceName(),
                entity.getDbType(),
                entity.getHost(),
                entity.getPort(),
                entity.getDatabaseName(),
                entity.getUsername(),
                entity.getPasswordRef(),
                entity.getConnectionParams(),
                entity.isActive()
        );
    }

    private DataSourceDetailsResponse toDetailsResponse(RegisteredDataSource source) {
        return new DataSourceDetailsResponse(
                source.id(),
                source.sourceCode(),
                source.sourceName(),
                source.dbType(),
                source.host(),
                source.port(),
                source.databaseName(),
                source.username(),
                source.connectionParams(),
                source.active()
        );
    }
}
