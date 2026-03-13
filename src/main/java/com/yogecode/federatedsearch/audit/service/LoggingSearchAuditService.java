package com.yogecode.federatedsearch.audit.service;

import com.yogecode.federatedsearch.audit.entity.SearchAuditEntity;
import com.yogecode.federatedsearch.audit.repository.SearchAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingSearchAuditService implements SearchAuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSearchAuditService.class);

    private final SearchAuditRepository searchAuditRepository;

    public LoggingSearchAuditService(SearchAuditRepository searchAuditRepository) {
        this.searchAuditRepository = searchAuditRepository;
    }

    @Override
    public void record(SearchAuditRecord auditRecord) {
        try {
            SearchAuditEntity auditEntity = new SearchAuditEntity();
            auditEntity.setRequestPayload(auditRecord.requestPayload());
            auditEntity.setResponsePayload(auditRecord.responsePayload());
            auditEntity.setRequestReceiveTime(auditRecord.requestReceiveTime());
            auditEntity.setResponseTime(auditRecord.responseTime());
            auditEntity.setQuery(auditRecord.query());
            auditEntity.setAuthor(auditRecord.author());
            auditEntity.setClientId(auditRecord.clientId());
            auditEntity.setResponseStatus(auditRecord.responseStatus());
            auditEntity.setHttpStatus(auditRecord.httpStatus());
            searchAuditRepository.save(auditEntity);

            LOGGER.info(
                    "search audit saved status={} httpStatus={} author={} clientId={}",
                    auditRecord.responseStatus(),
                    auditRecord.httpStatus(),
                    auditRecord.author(),
                    auditRecord.clientId()
            );
        } catch (Exception exception) {
            LOGGER.warn("Failed to persist search audit", exception);
        }
    }
}

