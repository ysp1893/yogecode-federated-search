package com.yogecode.federatedsearch.metadata.entity;

import com.yogecode.federatedsearch.common.enums.StorageType;
import com.yogecode.federatedsearch.datasource.entity.DataSourceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "app_entity_metadata")
public class EntityMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_code", nullable = false, unique = true, length = 100)
    private String entityCode;

    @Column(name = "entity_name", nullable = false, length = 200)
    private String entityName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private DataSourceEntity source;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 50)
    private StorageType storageType;

    @Column(name = "object_schema", length = 255)
    private String objectSchema;

    @Column(name = "object_name", nullable = false, length = 255)
    private String objectName;

    @Column(name = "primary_key_field", length = 100)
    private String primaryKeyField;

    @Column(name = "business_label", nullable = false, length = 100)
    private String businessLabel;

    @Column(name = "root_searchable", nullable = false)
    private boolean rootSearchable;

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

    public String getEntityCode() {
        return entityCode;
    }

    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public DataSourceEntity getSource() {
        return source;
    }

    public void setSource(DataSourceEntity source) {
        this.source = source;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    public String getObjectSchema() {
        return objectSchema;
    }

    public void setObjectSchema(String objectSchema) {
        this.objectSchema = objectSchema;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getPrimaryKeyField() {
        return primaryKeyField;
    }

    public void setPrimaryKeyField(String primaryKeyField) {
        this.primaryKeyField = primaryKeyField;
    }

    public String getBusinessLabel() {
        return businessLabel;
    }

    public void setBusinessLabel(String businessLabel) {
        this.businessLabel = businessLabel;
    }

    public boolean isRootSearchable() {
        return rootSearchable;
    }

    public void setRootSearchable(boolean rootSearchable) {
        this.rootSearchable = rootSearchable;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

