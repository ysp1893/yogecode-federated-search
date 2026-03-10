package com.yogecode.federatedsearch.metadata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_field_metadata")
public class FieldMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entity_id", nullable = false)
    private EntityMetadataEntity entity;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "field_path", length = 255)
    private String fieldPath;

    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;

    @Column(name = "searchable", nullable = false)
    private boolean searchable;

    @Column(name = "returnable", nullable = false)
    private boolean returnable;

    @Column(name = "filterable", nullable = false)
    private boolean filterable;

    @Column(name = "business_alias", length = 100)
    private String businessAlias;

    @Column(name = "primary_key_flag", nullable = false)
    private boolean primaryKey;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EntityMetadataEntity getEntity() {
        return entity;
    }

    public void setEntity(EntityMetadataEntity entity) {
        this.entity = entity;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public void setFieldPath(String fieldPath) {
        this.fieldPath = fieldPath;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }

    public boolean isReturnable() {
        return returnable;
    }

    public void setReturnable(boolean returnable) {
        this.returnable = returnable;
    }

    public boolean isFilterable() {
        return filterable;
    }

    public void setFilterable(boolean filterable) {
        this.filterable = filterable;
    }

    public String getBusinessAlias() {
        return businessAlias;
    }

    public void setBusinessAlias(String businessAlias) {
        this.businessAlias = businessAlias;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

