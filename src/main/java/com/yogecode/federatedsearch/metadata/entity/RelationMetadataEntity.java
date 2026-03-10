package com.yogecode.federatedsearch.metadata.entity;

import com.yogecode.federatedsearch.common.enums.JoinStrategy;
import com.yogecode.federatedsearch.common.enums.RelationType;
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
import jakarta.persistence.Table;

@Entity
@Table(name = "app_entity_relation")
public class RelationMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "relation_code", nullable = false, unique = true, length = 100)
    private String relationCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_entity_id", nullable = false)
    private EntityMetadataEntity fromEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_entity_id", nullable = false)
    private EntityMetadataEntity toEntity;

    @Column(name = "from_field", nullable = false, length = 100)
    private String fromField;

    @Column(name = "to_field", nullable = false, length = 100)
    private String toField;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 50)
    private RelationType relationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "join_strategy", nullable = false, length = 50)
    private JoinStrategy joinStrategy;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRelationCode() {
        return relationCode;
    }

    public void setRelationCode(String relationCode) {
        this.relationCode = relationCode;
    }

    public EntityMetadataEntity getFromEntity() {
        return fromEntity;
    }

    public void setFromEntity(EntityMetadataEntity fromEntity) {
        this.fromEntity = fromEntity;
    }

    public EntityMetadataEntity getToEntity() {
        return toEntity;
    }

    public void setToEntity(EntityMetadataEntity toEntity) {
        this.toEntity = toEntity;
    }

    public String getFromField() {
        return fromField;
    }

    public void setFromField(String fromField) {
        this.fromField = fromField;
    }

    public String getToField() {
        return toField;
    }

    public void setToField(String toField) {
        this.toField = toField;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public void setRelationType(RelationType relationType) {
        this.relationType = relationType;
    }

    public JoinStrategy getJoinStrategy() {
        return joinStrategy;
    }

    public void setJoinStrategy(JoinStrategy joinStrategy) {
        this.joinStrategy = joinStrategy;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

