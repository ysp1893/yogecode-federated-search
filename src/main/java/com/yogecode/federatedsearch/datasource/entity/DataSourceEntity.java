package com.yogecode.federatedsearch.datasource.entity;

import com.yogecode.federatedsearch.common.enums.DatabaseType;
import com.yogecode.federatedsearch.common.persistence.JsonMapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "app_data_source")
public class DataSourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_code", nullable = false, unique = true, length = 100)
    private String sourceCode;

    @Column(name = "source_name", nullable = false, length = 200)
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "db_type", nullable = false, length = 50)
    private DatabaseType dbType;

    @Column(name = "host", nullable = false, length = 255)
    private String host;

    @Column(name = "port", nullable = false)
    private Integer port;

    @Column(name = "database_name", nullable = false, length = 255)
    private String databaseName;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @Column(name = "password_ref", nullable = false, length = 512)
    private String passwordRef;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "connection_params", nullable = false, length = 4000)
    private Map<String, Object> connectionParams = new HashMap<>();

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public DatabaseType getDbType() {
        return dbType;
    }

    public void setDbType(DatabaseType dbType) {
        this.dbType = dbType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordRef() {
        return passwordRef;
    }

    public void setPasswordRef(String passwordRef) {
        this.passwordRef = passwordRef;
    }

    public Map<String, Object> getConnectionParams() {
        return connectionParams;
    }

    public void setConnectionParams(Map<String, Object> connectionParams) {
        this.connectionParams = connectionParams == null ? new HashMap<>() : new HashMap<>(connectionParams);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

