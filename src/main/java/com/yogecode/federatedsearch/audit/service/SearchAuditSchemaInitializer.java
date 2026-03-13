package com.yogecode.federatedsearch.audit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Component
public class SearchAuditSchemaInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchAuditSchemaInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public SearchAuditSchemaInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseName = metaData.getDatabaseProductName();

            if (databaseName != null && databaseName.toLowerCase().contains("mysql")) {
                jdbcTemplate.execute("ALTER TABLE app_search_audit MODIFY COLUMN request_payload LONGTEXT");
                jdbcTemplate.execute("ALTER TABLE app_search_audit MODIFY COLUMN response_payload LONGTEXT");
            } else if (databaseName != null && databaseName.toLowerCase().contains("h2")) {
                jdbcTemplate.execute("ALTER TABLE app_search_audit ALTER COLUMN request_payload CLOB");
                jdbcTemplate.execute("ALTER TABLE app_search_audit ALTER COLUMN response_payload CLOB");
            }
        } catch (Exception exception) {
            LOGGER.debug("Search audit schema initialization skipped: {}", exception.getMessage());
        }
    }
}
