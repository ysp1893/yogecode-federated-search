package com.yogecode.federatedsearch.connector.spi;

import com.yogecode.federatedsearch.common.enums.DatabaseType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ConnectorRegistry {

    private final Map<DatabaseType, SourceConnector> connectors = new EnumMap<>(DatabaseType.class);

    public ConnectorRegistry(List<SourceConnector> sourceConnectors) {
        sourceConnectors.forEach(connector -> connectors.put(connector.supports(), connector));
    }

    public SourceConnector get(DatabaseType databaseType) {
        SourceConnector connector = connectors.get(databaseType);
        if (connector == null) {
            throw new IllegalArgumentException("No connector available for database type: " + databaseType);
        }
        return connector;
    }
}

