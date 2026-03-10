# Federated Search Starter

Starter Spring Boot project for a metadata-driven federated search service that can register multiple data sources, map business entities to physical tables or collections, and assemble cross-source JSON responses.

## Current scope

- Spring Boot 3 / Java 21 / Maven
- Admin APIs for datasource and metadata registration
- Search API contract and orchestration skeleton
- Connector SPI for SQL and MongoDB implementations
- In-memory placeholder services for fast iteration

## Package layout

```text
src/main/java/com/example/federatedsearch
  api/
  audit/
  common/
  config/
  connector/
  datasource/
  metadata/
  planner/
  search/
  security/
```

## Next steps

1. Replace in-memory services with JPA-backed persistence.
2. Add secret encryption and external secret manager integration.
3. Implement SQL and Mongo query execution in connector modules.
4. Add relation-based result expansion in the planner and assembler.

