package com.yogecode.federatedsearch.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "app_search_audit")
public class SearchAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "request_payload", nullable = false)
    private String requestPayload;

    @Lob
    @Column(name = "response_payload", nullable = false)
    private String responsePayload;

    @Column(name = "request_receive_time", nullable = false)
    private Instant requestReceiveTime;

    @Column(name = "response_time", nullable = false)
    private Instant responseTime;

    @Column(name = "query_text", length = 500)
    private String query;

    @Column(name = "author", nullable = false, length = 100)
    private String author;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "response_status", nullable = false, length = 20)
    private String responseStatus;

    @Column(name = "http_status", nullable = false)
    private Integer httpStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    public Instant getRequestReceiveTime() {
        return requestReceiveTime;
    }

    public void setRequestReceiveTime(Instant requestReceiveTime) {
        this.requestReceiveTime = requestReceiveTime;
    }

    public Instant getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Instant responseTime) {
        this.responseTime = responseTime;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }
}
