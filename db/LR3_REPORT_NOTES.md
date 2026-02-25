# LR3 Report Notes

## 1) HikariCP connection pool

HikariCP is wired for JDBC-based DB access in WebSocket auth validation (`AuthWsUtil` via `HikariDataSourceProvider`).

Runtime parameters (from `application.properties`, system properties, or environment):

- `app.hikari.enabled`
  - `false` by default, enables/disables Hikari usage.
- `app.hikari.jdbc-url`
  - JDBC URL, example: `jdbc:postgresql://127.0.0.1:5432/tickets`.
- `app.hikari.username`
  - DB username.
- `app.hikari.password`
  - DB password.
- `app.hikari.maximum-pool-size`
  - Upper bound of concurrently opened DB connections.
- `app.hikari.minimum-idle`
  - Minimum number of idle connections kept ready.
- `app.hikari.connection-timeout-ms`
  - Max wait time for a free connection.
- `app.hikari.idle-timeout-ms`
  - Idle connection eviction timeout.
- `app.hikari.max-lifetime-ms`
  - Max lifetime of a connection before recycling.
- `app.hikari.pool-name`
  - Logical name used in logs/monitoring.

## 2) L2 JPA cache (Infinispan)

L2 cache is enabled in `persistence.xml` and entities are marked with `@Cacheable`:

- `Ticket`
- `Event`
- `Person`
- `Venue`
- `AppUser`

Main settings:

- `shared-cache-mode = ENABLE_SELECTIVE`
  - only entities marked by `@Cacheable` are stored in L2.
- `hibernate.cache.use_second_level_cache = true`
  - enables L2 cache.
- `hibernate.cache.use_query_cache = true`
  - enables Hibernate query cache region usage.
- `hibernate.cache.region.factory_class = infinispan`
  - chooses Infinispan cache provider.
- `hibernate.cache.infinispan.cfg = infinispan-l2.xml`
  - external cache config file.
- `hibernate.generate_statistics = true`
  - enables counters (hit/miss/put) used for logging.

`infinispan-l2.xml` tuning:

- `statistics="true"` enables cache metrics.
- `memory max-count="10000"` limits number of cached entries.
- `expiration max-idle="600000"` evicts entries after 10 min idle.

Effect on storage level: frequently reused entity snapshots are served from L2 cache instead of re-reading from DB, reducing DB I/O under repeated reads.

## 3) Toggleable L2 cache statistics logging

Implementation uses CDI interceptor:

- Binding: `@LogL2CacheStats`
- Interceptor: `L2CacheStatsLoggingInterceptor`
- Runtime switch service: `L2CacheStatsLoggingService`

Admin API:

- `GET /api/cache/l2/stats-logging`
- `PUT /api/cache/l2/stats-logging` body `{ "enabled": true|false }`

When enabled, each intercepted read method logs:

- delta `hits/misses/puts` for current call
- total `hits/misses/puts`

## 4) MinIO storage + distributed transaction for CSV import

Uploaded CSV files are stored in MinIO bucket (`app.minio.*` settings). Successful import history entries contain file metadata and support download from:

- `GET /api/tickets/import/history/{operationId}/file`

### Business-level 2PC flow

1. **Prepare phase**
   - CSV uploaded to MinIO staging key (`imports-staging/...`).
2. **DB phase**
   - ticket insertions happen in DB transaction.
   - import operation is marked `SUCCESS` in same DB transaction with final object key metadata.
3. **Commit phase (beforeCompletion)**
   - staged object is copied to final MinIO key (`imports/...`).
   - if copy fails, DB transaction rolls back.
4. **After-completion**
   - on commit: staging object is removed;
   - on rollback: staging/final objects are cleaned, operation is marked `FAILED` in a new transaction.

This guarantees transactional coupling between DB inserts and file persistence.

## 5) Failure scenarios for defense

1. **MinIO failure, DB alive**
   - stop MinIO before import;
   - prepare phase fails -> DB import transaction is not committed.
2. **DB failure, MinIO alive**
   - stop DB during import;
   - DB rollback triggers MinIO cleanup in `afterCompletion`.
   - for restricted environments (no DB service control), use `simulateDbFailure=true` to inject `PersistenceException` before first repository call.
3. **RuntimeException in server logic**
   - call import with `simulateFailure=true` query param;
   - exception thrown after DB flush, before external commit finalization;
   - DB is rolled back and staged file is removed.

## 6) Parallel requests behavior (JMeter)

Possible concurrent situations and handling:

- Two users import CSV with overlapping unique ticket fields (`name`, `coordinates`):
  - handled by serializable isolation + advisory locks (`pg_advisory_xact_lock`) + explicit uniqueness checks.
- Multiple imports in parallel writing independent data:
  - each import has unique operation id and unique MinIO object keys; no cross-request object collision.
- Concurrent reads of import history while imports are running:
  - history entries show `IN_PROGRESS` until transaction completion; terminal statuses updated in dedicated transaction boundaries.

## 7) Environment shutdown requirement

After demo, stop all components:

- application server (Jakarta app)
- PostgreSQL
- MinIO
- load tools (JMeter) if started
