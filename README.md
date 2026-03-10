# Yogecode Federated Search

A metadata-driven Spring Boot service for federated search across multiple SQL and NoSQL data sources.

## Overview

This project is a starter platform for building a federated search engine that can:

- register heterogeneous data sources at runtime
- store datasource, entity, field, relation, and keyword metadata
- resolve business keywords like `user`, `customer`, or `order` to configured entities
- orchestrate cross-source search requests and assemble unified JSON responses
- expose admin and search APIs through OpenAPI and Swagger UI

The current implementation focuses on the metadata and orchestration layer. Query execution connectors for MySQL, PostgreSQL, and MongoDB are present as starter adapters and can be extended with real runtime query logic.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- H2 for local metadata storage
- Spring Data MongoDB
- OpenAPI / Swagger UI

## Current Capabilities

- Dynamic datasource registration API
- Metadata registration APIs for entities, fields, relations, and keywords
- Metadata inspection APIs for datasources, entities, fields, relations, and keywords
- JPA-backed persistence for metadata
- In-memory cache layer for fast metadata lookups
- Search API skeleton with planner and connector registry
- Swagger UI for interactive API exploration

## API Docs

When the application is running:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Project Structure

```text
src/main/java/com/yogecode/federatedsearch
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

## Local Run

Compile:

```bash
./apache-maven-3.9.9/bin/mvn -q -DskipTests compile
```

Run:

```bash
./apache-maven-3.9.9/bin/mvn spring-boot:run
```

## Example Flow

1. Register a datasource
2. Register an entity mapped to a table or collection
3. Register fields for that entity
4. Register relations to other entities
5. Register business keywords
6. Call `/api/search` with a structured request

## Roadmap

- Implement real SQL and Mongo query execution in connectors
- Add relation-based expansion in search results
- Add schema discovery helpers for connected data sources
- Add secret management for datasource credentials
- Add tests for planner, metadata services, and API controllers
