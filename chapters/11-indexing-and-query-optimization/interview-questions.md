# Interview Questions: Indexing and Query Optimization

**Bank size:** 10  
**Rationale:** Focused read-path chapter (indexes, plans, hot queries); rubric default for narrow data-path topics.  
**Last updated:** 2026-05-20

---

## Core

## 1. What problem does a B-tree (or B+tree) index solve that a full table scan does not?

**Answer:** An ordered index lets the engine **seek** to a key range in O(log n) page reads instead of scanning every row—critical when a table has millions of rows but a query filters on a selective predicate (e.g., `user_id = ?` on orders). Pain: p99 API latency jumps from ~5 ms to seconds when a dashboard query loses its index. Trade-off: every index adds **write amplification** and storage; you optimize **read paths you measure**, not every column.

---

## 2. When would you choose a hash index over a B-tree, and when would you avoid it?

**Answer:** Hash indexes excel at **equality lookups** (`id = ?`) with fixed key width; they do not support range scans or `ORDER BY` on the indexed column. Use for point lookups on very hot keys (some engines expose this explicitly); avoid when queries need **ranges, sorting, or prefix LIKE**. In Postgres, hash indexes are rare in production; B-tree + selective predicates usually win. Architect takeaway: match index type to **access pattern in EXPLAIN**, not column name.

---

## 3. Explain “leftmost prefix” for a composite index `(tenant_id, created_at, status)`.

**Answer:** The index is ordered first by `tenant_id`, then `created_at` within tenant, then `status`. Queries that filter on **`tenant_id` alone** or **`tenant_id` + `created_at`** can use the index; filtering only on `created_at` or `status` generally **cannot** (unless you add a different index). Production mistake: index `(status, tenant_id)` for `WHERE tenant_id = ? AND status = ?`—planner may scan more rows. Validate with `EXPLAIN (ANALYZE, BUFFERS)` on production-like cardinality.

---

## 4. What is a covering index, and why can it remove heap lookups?

**Answer:** A **covering** index includes all columns the query needs (via included columns or a composite that carries projections). The engine satisfies `SELECT id, amount, created_at WHERE user_id = ?` entirely from the index leaf pages—**index-only scan**—avoiding random heap fetches (often 10× slower on wide tables). Trade-off: wider indexes, more write cost. Use when one query is **>1% of DB CPU** or drives checkout latency.

---

## 5. How do you use EXPLAIN output in an incident where “the DB is slow”?

**Answer:** Start with **actual vs estimated rows** (bad stats → wrong nested loop), **seq scan on large tables**, **sort/hash spill to disk**, and **buffer hits**. Compare p95 **query time** in APM/logs to plan shape. Common fixes: `ANALYZE`, add/reorder index, rewrite N+1 to batch join, cap `LIMIT`, or move aggregation to a **materialized view** ([Chapter 10](../10-database-design-and-data-modeling/README.md)). Avoid blindly `SET enable_seqscan = off`—that hides the real access path.

---

## 6. An ORM loads 500 `Order` entities then lazy-loads `LineItem` per order. What is happening at the database layer?

**Answer:** Classic **N+1**: 1 query for orders + 500 queries for line items—~501 round trips (~1–50 ms each → seconds). Fix with **join fetch**, `IN (...)` batch, or a read model query. At scale this looks like “DB CPU is fine but API timeout”—connection pool exhaustion from query count. Measure: ORM SQL log count per request; target **O(1) queries** per API for hot paths.

---

## Stretch

## 7. Product wants `WHERE LOWER(email) = ?`. How do you index without killing writes?

**Answer:** A function on the column prevents a plain B-tree on `email` from being used. Options: **generated/stored column** `email_lower` with index on that; **citext** (Postgres); or accept scan if table is small. Avoid full-table functional indexes on high-write tables without need. Trade-off: schema migration vs. application normalization (store lowercased email on write). Document **uniqueness** on the normalized form to prevent duplicate accounts.

---

## 8. When is it correct to *not* add an index?

**Answer:** Skip when: column has **low cardinality** alone (e.g., boolean `is_deleted` on 50/50 split) unless combined in a selective composite; table is **tiny** and always cached; write rate is extreme and read is rare; or workload is **append-only analytics** better served by columnar/warehouse. Too many indexes slow **INSERT/UPDATE** and vacuum/merge—payment ledger tables often keep indexes minimal and use **partition pruning** instead.

---

## 9. Orders table: 200M rows, query `WHERE user_id = ? ORDER BY created_at DESC LIMIT 20`. Design the index.

**Answer:** Composite **`(user_id, created_at DESC)`** (or ASC with backward scan) matches filter + sort—planner reads 20 index entries, not 200M. If you only index `user_id`, the engine may fetch all user rows then sort. Consider **partitioning by time** if retention is tiered. Numbers: selective `user_id` → thousands of rows per user worst case—index order matters for LIMIT. Pair with connection pool and statement timeout (~2–5 s upper bound for OLTP).

---

## 10. After a deploy, one query regressed from 5 ms to 800 ms. Walk your debugging steps.

**Answer:** (1) Confirm **same SQL text** and parameters (plan cache pollution). (2) `EXPLAIN ANALYZE` in staging with prod stats if possible. (3) Check **row count drift**, missing index, or **stats stale** after bulk load. (4) Lock/contention: `pg_locks`, long transactions holding snapshots. (5) Infrastructure: replica lag routing reads to stale replica isn’t slower—but **wrong replica** under load is. (6) Rollback or `pg_hint_plan` only as temporary; fix root cause and add **query latency alert** on that fingerprint. Cross-link: observability ([Chapter 26](../26-observability/README.md)).
