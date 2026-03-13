package com.yogecode.federatedsearch.audit.service;

import java.time.Instant;

public record SearchAuditRecord(
        String requestPayload,
        String responsePayload,
        Instant requestReceiveTime,
        Instant responseTime,
        String query,
        String author,
        Long clientId,
        String responseStatus,
        Integer httpStatus
) {
}
