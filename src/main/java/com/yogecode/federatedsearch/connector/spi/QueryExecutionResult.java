package com.yogecode.federatedsearch.connector.spi;

import java.util.List;
import java.util.Map;

public record QueryExecutionResult(
        String entityCode,
        List<Map<String, Object>> rows,
        long total
) {
}

