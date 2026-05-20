# Interview Questions: Database Design and Data Modeling

**Bank size:** 50  
**Rationale:** Core Phase B chapter (storage truth before cache/events); rubric 50 for multi-concept modeling, SQL/NoSQL, normalization, migrations, and sharding—user-requested top 50 with full answers.  
**Last updated:** 2026-05-20

---

## Foundations

## 1. What is the difference between a logical data model and a physical schema?

**Answer:** The **logical model** names entities, relationships, and constraints in business language (customer places order)—independent of Postgres vs Oracle. The **physical schema** maps that to tables, types, indexes, partitions, and storage parameters. Architects own the logical model in design reviews; teams implement physical schema with migration discipline. Drift happens when ORM-generated schemas skip FKs or when analysts build reports on tables that were never in the logical model.

---

## 2. Define 1NF, 2NF, and 3NF in one sentence each with a commerce example.

**Answer:** **1NF:** columns are atomic (no `tags` CSV in one cell—use `order_tags` table). **2NF:** no partial dependency on a composite key (if `(order_id, line_no)` is PK, `product_name` must not depend only on `line_no`). **3NF:** no transitive dependency (`order` → `customer_id` → `customer_name`; store `customer_id` on order, not duplicate city via zip unless denormalized on purpose). Checkout OLTP usually targets **3NF** on the ledger; dashboards denormalize elsewhere.

---

## 3. What is an aggregate in DDD and how does it map to database transactions?

**Answer:** An **aggregate** is a cluster of entities with one root (e.g., `Order` + `OrderLine`) updated through the root under **one consistency boundary**. In the DB, that maps to **one transaction** touching rows owned by that aggregate—avoid cross-aggregate updates in a single TX when services are split. Cross-aggregate consistency uses **events or sagas** ([Ch. 23](../23-idempotency-sagas-and-distributed-transactions/README.md)), not a single giant transaction across ten services.

---

## 4. Surrogate key vs natural key—when do you choose each?

**Answer:** **Surrogate** (UUID, bigint): stable when business attributes change, easy FK references, no leakage of business meaning. **Natural** (country code, immutable SKU): fewer joins when globally unique and truly immutable. Avoid surrogate **and** natural unique without reason. Payments often use surrogate `payment_id` plus unique `(provider, provider_ref)` for idempotency.

---

## 5. What problem do foreign keys solve that application-only checks do not?

**Answer:** FKs enforce **referential integrity** even when bugs, admin scripts, or a second service write orphans. Under concurrency, app-only checks race: two deletes/inserts interleave. FK cost: migration ordering, delete cascades must be designed, some sharded systems omit them and pay operational tax. For single-region OLTP money paths, **prefer FKs** unless sharding makes them impractical—then document compensating jobs.

---

## 6. Explain optimistic vs pessimistic concurrency at the row level.

**Answer:** **Optimistic:** read row with `version`, update with `WHERE version = ?`; on zero rows updated, retry or 409. Good for **low collision** (profile edits). **Pessimistic:** `SELECT ... FOR UPDATE` holds row lock until commit. Good for **hot inventory** if transactions are short. Failure modes: optimistic **retry storms**; pessimistic **deadlocks** and lock waits—monitor both.

---

## 7. What is an access path?

**Answer:** The **query shape** the product needs repeatedly—e.g., `orders WHERE customer_id = ? ORDER BY created_at DESC LIMIT 20`. Schema and indexes should serve access paths; normalization alone does not. Missing access path design forces **N+1 ORM** or giant joins; teams then cache and fight invalidation. List access paths per API in design reviews before choosing denormalization.

---

## 8. What is the system of record?

**Answer:** The **authoritative store** for a fact—if cache, search, and warehouse disagree, the SoR wins on reconcile. Usually OLTP Postgres for orders/payments. Derived stores (Redis, OpenSearch, Snowflake) must have **rebuild story** from SoR. Anti-pattern: treating cache as SoR for inventory without write-through discipline.

---

## 9. What is expand–contract schema migration?

**Answer:** **Expand:** add new column/table nullable or dual-write compatible. **Migrate:** backfill, dual-read/write. **Contract:** remove old column after consumers switch. Avoid destructive `RENAME`/`DROP` in one deploy on hot tables. Tools: Flyway/Liquibase, pg-osc, gh-ost for MySQL. Measure migration **lock duration** and **replica lag** during backfill.

---

## 10. What is row-level security (RLS) and when is it worth it?

**Answer:** **RLS** filters rows per DB session (`tenant_id = current_setting(...)`). Worth it for **multi-tenant SaaS** defense in depth when many services share one DB—reduces cross-tenant leak if one query forgets `tenant_id`. Not a substitute for app checks; test with integration tests. Ops cost: connection pool must set session vars; debugging harder.

---

## Application

## 11. Model tables for a one-to-many Order → OrderLine relationship. What keys and constraints?

**Answer:** `orders(id PK, customer_id FK, status, created_at, version)` and `order_lines(id PK, order_id FK NOT NULL, product_id FK, qty, unit_price_cents, UNIQUE(order_id, line_no))`. FK `order_id` → `orders(id)` with `ON DELETE RESTRICT` (or cascade only if lines have no independent lifecycle). Index `(customer_id, created_at DESC)` for list access path. Never update `order_id` on a line—delete and re-add if business allows.

---

## 12. How do you model optional one-to-one (Order → Shipment)?

**Answer:** `shipments(order_id PK/FK UNIQUE, carrier, tracking_number, shipped_at)`—**shared PK** or unique FK on `order_id` enforces 0..1. Alternative: nullable `shipment_id` on `orders` inverts ownership if shipment is created first rarely. Pick ownership by which aggregate creates the row in the happy path.

---

## 13. Many-to-many Product ↔ Category with sort order per category?

**Answer:** Junction table `category_products(category_id FK, product_id FK, sort_rank, PRIMARY KEY(category_id, product_id))`. Index `(category_id, sort_rank)` for browse path. Avoid comma-separated category ids on `products`—breaks 1NF and indexing.

---

## 14. Where do you store idempotency keys for payment APIs?

**Answer:** Table `idempotency_keys(key PK/hash, request_hash, response_body, status, created_at)` with **TTL job** or partition by date. Unique on `key` makes retries safe. Store **final response** for replay. Retention: 24–72h typical, match PSP retry window. Index `created_at` for cleanup. Do not rely only on Redis without durability story for money.

---

## 15. How model soft delete vs hard delete?

**Answer:** **Soft:** `deleted_at` nullable, unique constraints often need **partial unique index** (`WHERE deleted_at IS NULL`). Queries must filter `deleted_at IS NULL` everywhere—easy to leak in ORM. **Hard:** `DELETE` with FK cascade or archive table. Compliance/audit favors soft or **append-only archive**. Search indexes need tombstone sync.

---

## 16. JSON column vs child table for evolving product attributes?

**Answer:** **JSONB** (Postgres): fast iteration, index `attributes->>'color'` for hot filters. **Child table** `product_attributes(key, value)`: better constraints and SQL analytics. Rule: if you **filter/join** on attribute, promote to column or indexed JSON path; if rare metadata, JSON is fine. Avoid 1MB JSON blobs in hot rows—TOAST and buffer bloat.

---

## 17. How do you represent money in schema?

**Answer:** **`BIGINT` minor units** (cents) per currency column, never `FLOAT`. Store `currency CHAR(3)` with amount. Display rounding in locale layer. For multi-currency ledgers, separate `amount` and `currency` plus FX metadata table if needed. Check constraints `amount >= 0` where business forbids negative.

---

## 18. Model audit history: append-only table vs temporal tables?

**Answer:** **`entity_audit(id, entity_id, changed_by, diff JSON, at)`** append-only is portable and stream-friendly. **Temporal/system-versioned** tables (SQL:2011) give `AS OF` queries with less app code but vendor-specific ops. High-write entities may sample audit or audit only financial fields. Immutable audit: no UPDATE/DELETE grants on audit table.

---

## Design & Trade-offs

## 19. When is denormalization justified in the OLTP database?

**Answer:** When measured **read latency or join cost** blocks SLA and the duplicated field is **stable** or **cheap to refresh**—e.g., `product_title` on `order_lines` snapshot at purchase time (intentional historical copy). Not justified for “avoid joins” on mutable catalog fields without update strategy. Prefer **materialized view** or **read model** if staleness of seconds/minutes is OK.

---

## 20. Compare single shared database vs database-per-service.

**Answer:** **Shared DB:** simpler joins and transactions, **coupled deploys**, schema ownership fights—acceptable early or inside one bounded context. **DB per service:** independent scale and schema, cross-service queries via **API/events only**—requires sagas and idempotency. Architect rule: **one writer per table family** even before full split; events for read sides.

---

## 21. PostgreSQL vs MySQL as default OLTP in 2026—what tilts the choice?

**Answer:** Both viable. **Postgres:** richer types (JSONB, arrays), partial indexes, extensions, often preferred greenfield. **MySQL/InnoDB:** huge ops familiarity, managed Aurora ecosystem. Decide on **team ops**, **managed offering**, **specific features** (GIS, RLS), not blog myths. Migration between them is expensive—treat as multi-year decision.

---

## 22. When is MongoDB/document store a good primary database?

**Answer:** When documents map 1:1 to **aggregate read shape**, schema varies widely per record, **cross-document ACID** is rare, and team owns index design. Weak fit: **double-entry ledger**, inventory invariants across documents, heavy relational reporting without ETL. If you need multi-document transactions often, revisit SQL.

---

## 23. Redis as primary store—acceptable use cases?

**Answer:** **Ephemeral/session**, rate limits, feature flags with rebuild, **cache** with SoR elsewhere. Not acceptable as **sole** store for payments without AOF/cluster durability story and backup drills. Redis + Postgres: Redis holds lock/session; Postgres holds truth.

---

## 24. Normalized schema vs wide “document in Postgres” JSON for orders?

**Answer:** **Normalized** wins for integrity, partial updates, and BI. **Wide JSON order blob** wins for read-once write-rare prototypes—painful for line-level refunds, reporting, and partial indexes. Hybrid: normalized lines + JSON for **vendor extensions**. Production checkout almost always normalizes lines.

---

## 25. UUID vs bigint primary keys for high-insert tables?

**Answer:** **Bigint identity:** compact indexes, sequential insert locality. **UUIDv4:** random insert hurts btree locality; **UUIDv7** time-sortable improves locality. **UUID** helps offline client creation and merge. At **>10k inserts/s** on one table, measure index bloat and consider sequences or sharded id generators.

---

## 26. Multi-tenancy: shared schema with tenant_id vs schema-per-tenant?

**Answer:** **Shared + tenant_id:** simplest ops, risk of leaky queries—use RLS + mandatory filters. **Schema-per-tenant:** better isolation, migration fan-out. **DB-per-tenant:** enterprise/regulatory, highest ops cost. Choose by **isolation requirement** and tenant count (10 vs 10k tenants).

---

## 27. Read replicas: what consistency caveats do APIs inherit?

**Answer:** Async replication → **replica lag** (ms–seconds). Users may not see their write on immediate refresh. Mitigations: **read-your-writes** route to primary after mutation, session stickiness with lag-aware token, or accept staleness in UI copy. Do not cache replica responses without TTL if lag spikes.

---

## 28. CQRS-lite: when to add a read model table?

**Answer:** When **one access path** dominates traffic and join cost is proven (dashboard, feed, search). Write path updates SoR; **async projector** fills `user_feed_items`. Trade-off: **eventual consistency**, projector lag monitoring, rebuild playbook. Not day-one unless metrics justify complexity.

---

## Coding

## 29. Write the SQL pattern for optimistic locking on an `inventory` row with `version`.

**Answer:** `UPDATE inventory SET on_hand = on_hand - $1, version = version + 1 WHERE sku = $2 AND version = $3 AND on_hand >= $1` — check `rows affected == 1`. Else reload `SELECT on_hand, version FROM inventory WHERE sku = $2` and retry with cap or return 409. Never `SELECT` then `UPDATE` without version in WHERE.

---

## 30. How implement idempotent insert for webhook `event_id` in Postgres?

**Answer:** `INSERT INTO payment_events (provider, event_id, payload) VALUES ($1,$2,$3) ON CONFLICT (provider, event_id) DO NOTHING` or catch unique violation. Return 200 to provider on duplicate. Unique index must exist before traffic. Optionally store processed outcome for replay of same response body.

---

## 31. Detect N+1 queries from an ORM in order listing—what do you look for?

**Answer:** Trace shows **1 query for orders + N for lines or products**. Fix: `JOIN FETCH`, eager batch `WHERE order_id IN (...)`, or DTO query with explicit join. Schema fix: **denormalized snapshot** on line only if join remains hot after fetch tuning ([Ch. 11](../11-indexing-and-query-optimization/README.md)).

---

## 32. Pagination: OFFSET vs keyset for `orders BY created_at`?

**Answer:** **OFFSET** degrades O(offset) on large pages—bad for deep feeds. **Keyset:** `WHERE (created_at, id) < ($cursor_ts, $cursor_id) ORDER BY created_at DESC, id DESC LIMIT 20` with index `(customer_id, created_at DESC, id DESC)`. Stable ordering requires tie-breaker column.

---

## 33. SQL to find orders with no lines (data quality check)?

**Answer:** `SELECT o.id FROM orders o LEFT JOIN order_lines l ON l.order_id = o.id WHERE l.id IS NULL` — run in CI or nightly job. Prevent with FK from lines to orders and app invariant that lines created in same TX as order header.

---

## 34. Implement “transfer $X between accounts” in one transaction—sketch statements.

**Answer:** `BEGIN; UPDATE accounts SET balance = balance - $1, version = version + 1 WHERE id = $from AND version = $vfrom AND balance >= $1; UPDATE accounts SET balance = balance + $1, version = version + 1 WHERE id = $to AND version = $vto; INSERT INTO ledger (...); COMMIT;` — both updates must succeed or rollback. Use constraints `balance >= 0`.

---

## 35. How would you bulk-import 10M rows without locking checkout?

**Answer:** **COPY**/`LOAD` into staging table, no triggers on hot path, then `INSERT INTO ... SELECT` in batches off-peak, or merge with **SKIP LOCKED** workers. Disable synchronous triggers on staging; validate FKs after. Watch **WAL**, **replica lag**, **autovacuum**. Never one giant transaction.

---

## 36. Go/Java: handle `duplicate key` on insert as success for idempotency?

**Answer:** Java: catch `SQLIntegrityConstraintViolationException`, return duplicate outcome. Go: `ON CONFLICT DO NOTHING` or inspect `pq.Error` `Code == "23505"`. Metric `idempotency_duplicate_total`. Log at debug, not error—duplicates are expected under retries.

---

## System Design

## 37. Design the data model for a ride-hailing trip from request to completion (~50M trips/day order of magnitude).

**Answer:** **Hot OLTP:** `trips(id, rider_id, driver_id, status, geo, timestamps)` partitioned by `region` or time; `trip_events` append-only for state machine. **Geo index** separate (Redis/PostGIS) for matching—not every query on primary. **Access paths:** rider history `(rider_id, created_at)`, driver active trip `(driver_id) WHERE status IN (...)`. Writes **~600 QPS global average**, higher peaks per city—shard by `city_id` before single-node limits. Async: billing, ratings to separate tables/services.

---

## 38. E-commerce catalog 20M SKUs, product page p99 &lt; 100 ms—storage layout?

**Answer:** **OLTP Postgres:** normalized product core + inventory per SKU/warehouse. **Cache** (Redis) product page DTO keyed `product:{id}`. **Search** (OpenSearch) for browse/filter. Page read: cache → on miss SQL primary key + single round-trip for variants. Do not join 12 tables per request—**DTO projection** or cached blob with version. CDN for media URLs.

---

## 39. Chat messages 1B/day—can one Postgres hold it?

**Answer:** **~12k inserts/s average** globally—single Postgres is tight; **partition by conversation_id hash or time**, archive cold partitions to object storage. Hot access: `messages WHERE conversation_id = ? ORDER BY id DESC LIMIT 50` index `(conversation_id, id DESC)`. Attachments in S3. Consider **Cassandra/Dynamo** for append-only at extreme scale; keep **recent window** optimization.

---

## 40. Double-entry wallet balances—schema sketch?

**Answer:** `accounts(id, currency)`, `journal_entries(id, ts)`, `postings(entry_id, account_id, amount_cents CHECK amount != 0)` with constraint **sum of postings per entry = 0** (or debit/credit columns). Never update balance without entry; **materialized balance** cache optional with reconciliation job. Unique business refs on entries for idempotency.

---

## 41. SaaS analytics on operational DB—what breaks and fix?

**Answer:** Heavy reporting scans **contend with OLTP**, inflate lag, cause p99 spikes. Fix: **read replica** for BI, **CDC to warehouse** (Snowflake/BigQuery), or event stream. Freeze **large joins** on primary during peak. Contract: analytics is **eventually consistent** hours behind.

---

## 42. Global users table—single region Postgres vs multi-region?

**Answer:** Start **single region** with clear **RPO/RTO**; multi-region active-active for profile rows invites **conflict resolution**. If GDPR residency required, **shard by region** at app router. Replicate read-only globally only when product needs it—measure lag and conflict rules before Cockroach/Spanner cost.

---

## 43. Outbox pattern—table design?

**Answer:** `outbox(id, aggregate_id, event_type, payload JSON, created_at, published_at NULL)` in **same transaction** as business row update. Relay worker polls `WHERE published_at IS NULL ORDER BY id` with `FOR UPDATE SKIP LOCKED`, publishes to Kafka, marks published. Unique on business idempotency if consumers need exactly-once semantics downstream.

---

## 44. Migration zero-downtime add `NOT NULL` column `tax_code` to `orders`?

**Answer:** (1) `ADD COLUMN tax_code TEXT NULL`; (2) deploy app dual-write; (3) backfill `UPDATE orders SET tax_code = 'DEFAULT' WHERE tax_code IS NULL` in batches; (4) `ALTER COLUMN SET NOT NULL` after verified; (5) remove app default path. For huge tables use **pg-osc**/online DDL. Monitor locks and lag each phase.

---

## Debugging & Ops

## 45. Duplicate payment rows after deploy—first three DB checks?

**Answer:** (1) Unique index on `(provider, event_id)` exists and enabled? (2) App catching duplicate as success vs double insert path? (3) Logs for retry storm from gateway timeout—correlate with missing idempotency header. Query `SELECT provider, event_id, COUNT(*) ... HAVING COUNT(*) > 1`.

---

## 46. p99 read latency up; `pg_stat_statements` shows new sequential scan on `orders`?

**Answer:** Recent migration dropped index, stats stale, or new filter not in index plan. Run `EXPLAIN`, check `pg_stat_user_indexes`, `ANALYZE`, restore composite index matching access path. ORM change may have removed `customer_id` predicate—leaked full scan.

---

## 47. Deadlocks in inventory reservation—causes and mitigations?

**Answer:** Concurrent TX lock rows in **different order** (`sku A then B` vs `B then A`). Mitigate: **consistent lock ordering**, smaller TX scope, retry on `40P01`, reduce isolation where safe, partition hot SKU to queue. Metric: `deadlock_count`, trace lock waits.

---

## 48. Replica lag alert fires during batch job—response playbook?

**Answer:** Pause or throttle batch, move job to replica-fed warehouse, increase batch parallelism with smaller chunks, check long queries on primary (`pg_stat_activity`). Communicate **stale read** risk to product. Long-term: CDC instead of giant `UPDATE` on primary.

---

## Staff+

## 49. Two teams share one Postgres schema; migrations conflict weekly. Your recommendation?

**Answer:** Short term: **migration ownership board**, sequential pipeline, forbidden direct DDL. Medium: **schema per bounded context** even in one instance—separate migration repos. Long: **database per service** with events for cross-context reads. Measure lead time and incident rate; shared DB is a **organizational bottleneck**, not only technical.

---

## 50. Principal review: “We’ll pick the database after the MVP.” What do you insist on before launch?

**Answer:** Insist on **documented invariants** (money, inventory, idempotency), **access paths for launch APIs**, **migration expand–contract habit**, and **unique constraints for retries** even in MVP—switching SQL→NoSQL later rarely happens cheaply. Allow reversible choices (Postgres default) but not **undefined ownership** of rows or missing webhook dedup. Define **scale trigger** (write TPS, storage TB) that forces shard/read-model review.
