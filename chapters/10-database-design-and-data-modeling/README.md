# Chapter 10: Database Design and Data Modeling

> **One line:** The database is the contract for truth—model entities for integrity on writes, then shape **access paths** for how the product actually reads; cache and events only work if storage is right first.

## Why this matters in production

A marketplace ships a “seller dashboard” that joins twelve tables per page load. At 2k QPS the primary PostgreSQL CPU hits 90%, p99 read latency crosses 800 ms, and the team adds Redis—then fights **cache invalidation bugs** because the underlying model was never aligned to access paths. Stakeholders see **stale inventory**, **duplicate settlement rows** after webhook retries, and **schema migration fear** that blocks every feature.

Storage is the **system of record** before [Chapter 12: Caching Strategies](../12-caching-strategies/README.md) and [Chapter 18: Event-Driven Architecture](../18-event-driven-architecture/README.md). Wrong normalization, missing uniqueness, or a document store chosen for “flexibility” without transaction boundaries shows up as **money incidents** and **unbounded migration cost**—not as a slow query you can index away later ([Chapter 11](../11-indexing-and-query-optimization/README.md)).

## Core ideas

### Relational modeling: entities, keys, and invariants

**Intuition:** Tables are shared contracts between services and analysts; keys and constraints encode business rules the application must not be the only enforcer of.

| Element | Purpose | Production signal |
|---------|---------|-------------------|
| **Primary key** | Stable row identity | Surrogate `uuid` vs natural key (`email`) |
| **Foreign key** | Referential integrity | Orphan rows after partial failures |
| **Unique constraint** | Idempotency, dedup | Webhook double-post without unique index |
| **Check constraint** | Domain rules in DB | Negative `on_hand` despite “validation” in API |
| **NOT NULL** | Required facts | `NULL` propagation in reports |

**Surrogate keys** (UUID, bigint identity) decouple identity from mutable business attributes—useful when SKU rebrands or email changes. **Natural keys** as PK work when immutable and globally meaningful (`ISO country code`). For distributed IDs, prefer **time-sortable UUIDs** (UUIDv7) or **Snowflake-style** ids when index locality matters at insert-heavy rates.

Align tables with **bounded contexts** from [Chapter 03: Domain-Driven Design](../03-domain-driven-design-and-bounded-contexts/README.md): an `Order` aggregate’s consistency boundary should map to rows you update in **one transaction**, not a god-schema every service joins.

### Normalization vs access paths

**Intuition:** Normalization removes update anomalies; **access paths** are the queries your product actually runs—often one per screen or API.

| Form | Idea | When it shines |
|------|------|----------------|
| **1NF** | Atomic columns, no repeating groups | Baseline for SQL |
| **2NF / 3NF** | No partial / transitive dependencies | OLTP: orders, ledgers, inventory |
| **Denormalized** | Duplicate data for read speed | Dashboards, feeds, search docs |

**3NF** is the default for money paths: `order_lines` reference `products` by id, not copy `product_name` unless you accept drift. **Denormalize deliberately** into:

- **Materialized views** or **read models** refreshed async (CQRS-lite).
- **Projection tables** (`order_summary_by_customer`) owned by the service that owns writes.
- **Search / analytics** stores—not the primary ledger.

Anti-pattern: “denormalize everything in the OLTP schema” so every product rename is a **multi-table migration** under load.

See [normalization vs access paths](./diagrams/normalization-vs-access-paths.md).

### SQL vs NoSQL selection

**Intuition:** Pick the store that matches **transaction shape** and **dominant access pattern**, not the label on the resume.

| Need | Lean toward | Example |
|------|-------------|---------|
| Multi-row ACID, joins, reporting | **PostgreSQL / MySQL** | Orders + payments + inventory |
| Session, rate limit, ephemeral | **Redis** | Not system of record for money |
| Flexible nested catalog, rare cross-doc TX | **Document DB** | Product CMS; not double-entry ledger |
| Massive time-series, partition tolerance | **Wide-column** | Metrics, IoT at huge volume |
| Friend-of-friend, path queries | **Graph** | Fraud rings; not default cart DB |

**PACELC reminder** ([Chapter 21](../21-cap-theorem-and-pacelc/README.md)): document and wide-column stores often choose **availability + partition tolerance** with **eventual** cross-shard invariants—you must design compensations ([Chapter 23](../23-idempotency-sagas-and-distributed-transactions/README.md)).

Decision flow: [sql-nosql-selection](./diagrams/sql-nosql-selection.md).

### Schema design for APIs and evolution

**Intuition:** Migrations are production deploys; the API is a downstream consumer of your schema discipline.

- **Expand–contract** migrations: add nullable column → dual-write → backfill → enforce NOT NULL → remove old column. Never rename-in-place on hot tables without a transition period.
- **Version outward, not inward:** [Chapter 09: API Design](../09-api-design/README.md) can version `/v2` while DB carries both columns; don’t break mobile clients because `ALTER` was convenient.
- **Avoid shared-database integration** between microservices: one writer per table family; others consume events or query APIs.

**JSON columns** in Postgres: useful for **evolving attributes** with stable query keys on indexed fields; avoid storing entire aggregates with no schema discipline—you lose constraints and pay parse cost on hot filters.

### Partitioning, sharding, and multi-tenancy

At **single-region** scale, vertical scaling + read replicas + indexing often suffice to **~few k write TPS** on well-modeled Postgres. Plan sharding when:

- Single-node storage or write throughput is the ceiling (~TB hot set, sustained write saturation).
- Blast radius requires **tenant isolation** (noisy neighbor on shared tables).

**Partition keys:** `tenant_id`, `user_id`, or `order_id`—must match **every** hot query or you scatter-gather. **Cross-shard transactions** are expensive; design aggregates per shard ([Chapter 17: Microservices](../17-microservices-architecture/README.md)).

### Consistency on the write path

| Pattern | Use | Risk |
|---------|-----|------|
| **Optimistic locking** (`version` column) | Contested rows, short conflicts | Retry storms if UI hammers save |
| **Pessimistic `FOR UPDATE`** | Strict inventory in one TX | Lock waits, deadlocks |
| **Unique + idempotent insert** | Webhooks, client retries | Must handle duplicate as success |
| **Outbox table** | DB commit + event publish | Requires relay worker |

Code examples below show **optimistic version** updates and **unique-constraint idempotency**—patterns that belong in the schema, not only in application memory.

## When to use / when to avoid

**Use rigorous relational modeling when:**

- Money, inventory, or legal audit trails are involved.
- Multiple services must agree on the same facts (prefer one writer + events).
- Ad-hoc analytics and BI expect SQL joins.

**Use specialized stores when:**

- Access pattern is a **single key** or **time range** at extreme scale.
- Data is **derived** and rebuildable from the ledger (search, recommendations).
- Graph or geo queries are the product core—not bolted onto OLTP.

**Avoid:**

- MongoDB (or similar) as default because “schema is flexible” when invariants are relational.
- Normalizing to 6NF in the request path while the UI needs one document—push denorm to read models.
- Foreign keys disabled “for performance” without a documented ownership and cleanup story.
- Storing blobs in row pages that bloat buffers and slow scans—object storage + pointer.

## How it fails

| Symptom | Likely cause | What to check |
|---------|--------------|---------------|
| Duplicate charges | No unique on idempotency key | `payment_events(provider, event_id)` |
| Lost inventory update | Read-modify-write without version | `UPDATE ... WHERE version = ?` rows affected |
| Migration took down prod | Blocking `ALTER`, full table rewrite | Online migration tools, expand–contract |
| Cache “fixed” DB | Wrong access path; cache hides hot join | `pg_stat_statements`, query shape |
| Orphan line items | Missing FK or async delete race | Referential integrity, outbox ordering |
| Tenant A sees B data | Missing `tenant_id` filter / RLS | Row-level security, integration tests |
| Replica lag reads | Read-your-writes not guaranteed | Route session to primary or version token |

**Debugging hooks:** `pg_stat_statements` (mean time, calls), lock waits (`pg_locks`), duplicate key rate, migration duration metrics, ORM N+1 detection in traces, row counts per partition.

## Architect takeaway

- **Decide:** System of record per entity; normalization level; partition key before 10× data; which reads hit replica vs primary; idempotency uniqueness in DB.
- **Measure:** Write TPS per table, p95 query time per access path, migration duration, conflict/retry rate on optimistic columns, replica lag.
- **Document in design review:** ER for aggregates in scope; access-path list per API; expand–contract plan; SQL vs NoSQL rationale with rejected alternatives; failure behavior on unique violation and version conflict.

## Diagrams

- [Storage in the request path](./diagrams/overview.md) — primary, replicas, cache, events
- [Normalization vs access paths](./diagrams/normalization-vs-access-paths.md) — 3NF core vs read projections
- [SQL vs NoSQL selection](./diagrams/sql-nosql-selection.md) — decision flowchart

## Code examples

| Scenario | Java | Go |
|----------|------|-----|
| Optimistic concurrency on inventory row | [java/OptimisticVersionUpdate.java](./java/OptimisticVersionUpdate.java) | [go/optimistic_version_update.go](./go/optimistic_version_update.go) |
| Idempotent webhook insert via uniqueness | [java/UniqueConstraintIdempotentInsert.java](./java/UniqueConstraintIdempotentInsert.java) | [go/unique_constraint_idempotent_insert.go](./go/unique_constraint_idempotent_insert.go) |

**Production note:** Put **unique constraints** and **version columns** in migrations before shipping retrying clients or webhooks. Return **409 Conflict** on version mismatch with the current etag/version so UIs can refresh—mirror [Chapter 09](../09-api-design/README.md) error semantics.

## Related topics

- [Chapter 03: Domain-Driven Design and Bounded Contexts](../03-domain-driven-design-and-bounded-contexts/README.md) — aggregates, ownership, ubiquitous language → tables
- [Chapter 09: API Design](../09-api-design/README.md) — idempotency keys, pagination, contract vs schema evolution
- [Chapter 11: Indexing and Query Optimization](../11-indexing-and-query-optimization/README.md) — B-trees, plans, hot queries on your access paths
- [Chapter 12: Caching Strategies](../12-caching-strategies/README.md) — cache-aside after correct storage shape
- [Chapter 18: Event-Driven Architecture](../18-event-driven-architecture/README.md) — outbox, projections, read models
- [Chapter 23: Idempotency, Sagas, and Distributed Transactions](../23-idempotency-sagas-and-distributed-transactions/README.md) — cross-service money flows

## Interview preparation

See [interview-questions.md](./interview-questions.md) (**50** questions with full answers — modeling, SQL/NoSQL, normalization, migrations, sharding, and ops).
