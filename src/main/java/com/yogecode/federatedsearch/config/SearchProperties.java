package com.yogecode.federatedsearch.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.search")
public class SearchProperties {

    @Min(1)
    private int defaultPageSize = 20;

    @Min(1)
    @Max(500)
    private int maxPageSize = 100;

    @Min(100)
    private long sourceTimeoutMs = 3000;

    @Min(1)
    @Max(32)
    private int relationExecutorThreads = 8;

    @Min(10)
    @Max(2000)
    private int joinBatchSize = 500;

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public long getSourceTimeoutMs() {
        return sourceTimeoutMs;
    }

    public void setSourceTimeoutMs(long sourceTimeoutMs) {
        this.sourceTimeoutMs = sourceTimeoutMs;
    }

    public int getRelationExecutorThreads() {
        return relationExecutorThreads;
    }

    public void setRelationExecutorThreads(int relationExecutorThreads) {
        this.relationExecutorThreads = relationExecutorThreads;
    }

    public int getJoinBatchSize() {
        return joinBatchSize;
    }

    public void setJoinBatchSize(int joinBatchSize) {
        this.joinBatchSize = joinBatchSize;
    }
}

