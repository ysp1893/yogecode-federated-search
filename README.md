# Yogecode Federated Search

A metadata-driven Spring Boot service for federated search across multiple SQL and NoSQL data sources.

## Overview

This project lets you register datasources and metadata at runtime, then execute structured search requests against configured business entities.

Current focus:
- MySQL runtime query execution
- entity and relation metadata management
- relation expansion in search responses
- local + Redis-backed metadata cache
- cache refresh APIs

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Data Redis
- H2 for local metadata storage
- Spring Data MongoDB
- OpenAPI / Swagger UI

## Current Capabilities

- Dynamic datasource registration API
- Metadata registration APIs for entities, fields, relations, and keywords
- Metadata inspection APIs for datasources, entities, fields, relations, and keywords
- MySQL connector with `EQ`, `NE`, `GT`, `GTE`, `LT`, `LTE`, `LIKE`, `IN`, `NOT_IN`, `IS_NULL`, `IS_NOT_NULL`
- Relation expansion for included entities
- Per-entity field selection in search requests
- Relation-aware filters like `customer.status` or `cdr.CDRID`
- Local cache with Redis fallback
- Manual cache refresh APIs
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
  cache/
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

## Redis Sentinel Configuration

Redis Sentinel is configured through `application.yml`.

```yaml
spring:
  data:
    redis:
      password: ${REDIS_PASSWORD:}
      sentinel:
        master: ${REDIS_SENTINEL_MASTER:mymaster}
        nodes: ${REDIS_SENTINEL_NODES:192.168.24.31:26379}
```

Example environment values:

```bash
REDIS_SENTINEL_MASTER=mymaster
REDIS_SENTINEL_NODES=192.168.24.31:26379,192.168.24.32:26379,192.168.24.33:26379
REDIS_PASSWORD=
```

If Redis is unavailable, local cache still works. You can disable Redis cache reads and writes with:

```yaml
app:
  cache:
    redis:
      enabled: false
```

## Admin API Flow

Typical order:

1. Create datasource
2. Create entity
3. Create fields
4. Create relation
5. Search

### 1. Create Datasource

Endpoint:

```http
POST /api/admin/datasources
```

Payload:

```json
{
  "sourceCode": "my_local_mysql",
  "sourceName": "Local MySQL",
  "dbType": "MYSQL",
  "host": "localhost",
  "port": 3306,
  "databaseName": "adoptradiusbss",
  "username": "root",
  "password": "root",
  "connectionParams": {
    "ssl": false
  }
}
```

### 2. Create Entity

Endpoint:

```http
POST /api/admin/entities
```

Payload:

```json
{
  "entityCode": "customer",
  "entityName": "Customer",
  "sourceId": 2,
  "storageType": "TABLE",
  "objectSchema": "adoptradiusbss",
  "objectName": "tblcustomers",
  "primaryKeyField": "custid",
  "businessLabel": "customer",
  "rootSearchable": true
}
```

### 3. Create Fields

Endpoint:

```http
POST /api/admin/entities/{entityId}/fields
```

Payload:

```json
{
  "fields": [
    {
      "fieldName": "custid",
      "dataType": "LONG",
      "searchable": false,
      "returnable": true,
      "filterable": true,
      "businessAlias": "custId",
      "primaryKey": true
    },
    {
      "fieldName": "username",
      "dataType": "STRING",
      "searchable": true,
      "returnable": true,
      "filterable": true,
      "businessAlias": "userName",
      "primaryKey": false
    },
    {
      "fieldName": "cstatus",
      "dataType": "STRING",
      "searchable": true,
      "returnable": true,
      "filterable": true,
      "businessAlias": "customerStatus",
      "primaryKey": false
    }
  ]
}
```

### 4. Bind Two Tables

Example:
- `tblcustomers.username`
- `tbltacctcdr.UserName`

Endpoint:

```http
POST /api/admin/relations
```

Payload:

```json
{
  "relationCode": "customer_cdr",
  "fromEntityCode": "customer",
  "toEntityCode": "cdr",
  "fromField": "username",
  "toField": "UserName",
  "relationType": "ONE_TO_MANY",
  "joinStrategy": "IN"
}
```

## Bind Tables Across Two Databases

This is supported as long as both tables are registered as entities and each entity points to its own datasource.

Example:
- datasource 1: `crm_db.tblcustomers`
- datasource 2: `billing_db.tbltacctcdr`

Flow:
- create datasource for DB1
- create datasource for DB2
- create `customer` entity on DB1
- create `cdr` entity on DB2
- create relation between them

Example relation payload:

```json
{
  "relationCode": "customer_cdr_cross_db",
  "fromEntityCode": "customer",
  "toEntityCode": "cdr",
  "fromField": "username",
  "toField": "UserName",
  "relationType": "ONE_TO_MANY",
  "joinStrategy": "IN"
}
```

The current implementation executes the root query first, then loads related rows using the relation metadata.

## Search Request Shape

Endpoint:

```http
POST /api/search
```

Basic example:

```json
{
  "entity": "cdr",
  "entityFields": {
    "cdr": ["CDRID", "UserName", "AcctStatusType", "lastmodificationdate"],
    "customer": ["cstatus"]
  },
  "include": ["customer"],
  "page": 0,
  "size": 20
}
```

## Filter Operators

Current operators:

- `EQ`
- `NE`
- `GT`
- `GTE`
- `LT`
- `LTE`
- `LIKE`
- `IN`
- `NOT_IN`
- `IS_NULL`
- `IS_NOT_NULL`

### `EQ`

```json
{
  "field": "UserName",
  "operator": "EQ",
  "value": "p1@003"
}
```

### `NE`

```json
{
  "field": "UserName",
  "operator": "NE",
  "value": "p1@003"
}
```

### `LIKE`

```json
{
  "field": "UserName",
  "operator": "LIKE",
  "value": "%kumar%"
}
```

### `GTE`

```json
{
  "field": "createdAt",
  "operator": "GTE",
  "value": "2026-03-12T11:15:00"
}
```

### `LTE`

```json
{
  "field": "createdAt",
  "operator": "LTE",
  "value": "2026-03-12T11:30:00"
}
```

### `IN`

```json
{
  "field": "UserName",
  "operator": "IN",
  "value": ["kumar1144", "p1@003"]
}
```

### `NOT_IN`

```json
{
  "field": "UserName",
  "operator": "NOT_IN",
  "value": ["kumar1144", "p1@003"]
}
```

### `IS_NULL`

```json
{
  "field": "lastmodificationdate",
  "operator": "IS_NULL"
}
```

### `IS_NOT_NULL`

```json
{
  "field": "lastmodificationdate",
  "operator": "IS_NOT_NULL"
}
```

## Relation-Aware Filters

You can filter on included entities using `entityCode.fieldName`.

Example:

```json
{
  "entity": "customer",
  "filters": [
    {
      "field": "cdr.CDRID",
      "operator": "IS_NULL"
    }
  ],
  "entityFields": {
    "customer": ["username", "cstatus"],
    "cdr": ["CDRID"]
  },
  "include": ["cdr"],
  "page": 0,
  "size": 20
}
```

This is the closest current API equivalent to SQL patterns like:

```sql
left join tbltacctcdr d on d.UserName = c.username
where d.CDRID is null
```

Important:
- relation filters work only when that entity is present in `include`
- root filters are combined with `AND`
- relation filters are also combined with `AND`
- `ORDER BY` is not supported yet
- nested relation traversal is not supported yet

## GUI Date Range Suggestion

GUI can continue using the same search API and map date controls into `filters`.

Example for "last 15 minutes":

```json
{
  "entity": "cdr",
  "filters": [
    {
      "field": "createdAt",
      "operator": "GTE",
      "value": "2026-03-12T11:15:00"
    },
    {
      "field": "createdAt",
      "operator": "LTE",
      "value": "2026-03-12T11:30:00"
    },
    {
      "field": "username",
      "operator": "EQ",
      "value": "yogesh"
    }
  ],
  "page": 0,
  "size": 20
}
```

## Cache Refresh APIs

### Refresh Everything

```http
POST /api/admin/cache/refresh
```

### Refresh by Scope

```http
POST /api/admin/cache/refresh/{scope}?key=...
```

Supported scopes:

- `all`
- `metadata`
- `datasource`
- `entity`
- `relation`
- `keyword`
- `search-context`

Examples:

```http
POST /api/admin/cache/refresh/datasource?key=2
```

```http
POST /api/admin/cache/refresh/entity?key=customer
```

```http
POST /api/admin/cache/refresh/relation?key=customer
```

```http
POST /api/admin/cache/refresh/search-context?key=customer
```

## Current Limitations

- PostgreSQL connector is still a stub
- MongoDB connector is still a stub
- `ORDER BY` is not supported yet
- Complex `OR` groups are not supported yet
- Search result caching is not implemented; current cache is for datasource and metadata/search context

## Roadmap

- PostgreSQL runtime query execution
- MongoDB runtime query execution
- `ORDER BY` and richer query grammar
- `OR` conditions and grouped filters
- schema discovery helpers for connected data sources
- tests for planner, metadata services, cache flows, and API controllers
