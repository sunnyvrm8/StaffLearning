# Chapter 04: Data Structures and Complexity

> **One line:** Complexity is the language for predicting how your service behaves when traffic or catalog size doubles—before you profile, and especially after you profile.

## Why this matters in production

A marketplace search team ships “related products” by sorting every candidate SKU on each request. At 50 QPS it is fine; at 2k QPS p99 latency jumps from 40 ms to 800 ms and CPU saturates. The incident postmortem says “Elasticsearch was slow,” but the flame graph shows **O(n log n) sort per request** on 20k IDs already in memory. Stakeholders feel **checkout abandonment** and **infra cost**, not “we picked the wrong asymptotic class.”

Staff+ loops and on-the-job reviews expect you to **name dominant operations** (get, scan, rank, connect), **choose structures that match them**, and **separate algorithmic limits from I/O, locks, and GC**. That literacy feeds [Chapter 11: Indexing](../11-indexing-and-query-optimization/README.md) (B-trees, access paths), [Chapter 27: Performance Engineering](../27-performance-engineering/README.md) (tails, allocation), and [Chapter 28: Scalability](../28-scalability-and-capacity-planning/README.md) (hot keys, sharding)—you cannot reason about scale without complexity on the hot path.

## Core ideas

### Big-O is a contract with the future

**Intuition:** Big-O describes how work grows with input size **n** when n gets large—not the constant factor that wins at n = 50.

| Notation | Meaning | Production use |
|----------|---------|------------------|
| **O(1)** | Bounded work per op | Hash get, heap peek, array index |
| **O(log n)** | Halve problem each step | Balanced tree, binary search on sorted data |
| **O(n)** | Touch every element once | Single scan, BFS layer, hash build |
| **O(n log n)** | Sort-class | Full ranking, merge intervals via sort |
| **O(n²)** | Nested scans | Naive duplicate check; red flag at 10⁴+ rows |

**Amortized** matters when operations occasionally resize (dynamic array, hash rehash): **average** cost per insert may be O(1) while **worst** single insert is O(n). Document which you promise in SLAs—p99 cares about worst spikes.

**Hidden constants:** Cache locality often beats “better” Big-O on paper. A contiguous slice scan can beat a linked list at O(n) because pointer chasing misses L1/L2 cache (ties to [Chapter 07: Memory Management](../07-memory-management/README.md)).

### Arrays, lists, and hash tables

| Structure | Strength | Weakness | Typical hot path |
|-----------|----------|----------|------------------|
| **Contiguous array / slice** | Cache-friendly scan, index O(1) | Insert/delete middle O(n) | Metrics batch, columnar aggregates |
| **Linked list** | O(1) splice if you hold node ref | Poor scan, allocator churn | Rare in hot paths; LRU *order* with hash index |
| **Hash map / set** | Expected O(1) get/put | No order; rehash spikes; collision attacks | Session table, dedup, idempotency keys |
| **Dynamic array (ArrayList, Go slice)** | Amortized append | Copy on grow | Event buffer before flush |

**Load factor** on hash tables: as fill grows, probes increase; resizing copies all entries—watch **heap jump** after bulk import. In distributed systems, “hash” becomes **partition key** (links to Ch. 28 hot keys).

### Trees, heaps, and tries

| Structure | Ops | When |
|-----------|-----|------|
| **Binary search tree (balanced)** | O(log n) search/insert | In-memory ordered map when tree fits RAM |
| **B-tree / B+ tree** | Logarithmic with disk blocks | Database and index pages ([Ch. 11](../11-indexing-and-query-optimization/README.md)) |
| **Binary heap** | O(log n) push/pop, O(1) min/max | Top-K, schedulers, merge k sorted streams |
| **Trie** | O(key length) prefix | Autocomplete, routing tables, IP prefixes |

**Top-K pattern:** Maintain a **min-heap of size K** while scanning counts—O(n log K). Full sort is O(n log n); when K = 10 and n = 10⁶, the gap is operational.

### Graphs and Union-Find

**BFS/DFS** model dependencies: permission reachability, stale cache invalidation graphs, workflow steps. BFS gives **shortest unweighted path**; DFS suits cycle detection and topological sort (build order, task DAG).

**Union-Find (disjoint set)** tracks connected components with near–O(α(n)) merges—fraud rings, network connectivity, “are these accounts the same cluster?” Avoid O(n²) pairwise checks when edges stream in.

### Algorithmic patterns on arrays

| Pattern | Signal | Complexity |
|---------|--------|--------------|
| **Two pointers** | Sorted array, pair sum, dedup in place | O(n) |
| **Sliding window** | Contiguous subarray/substring constraint | O(n) |
| **Prefix sum** | Range sum queries after O(n) preprocess | Query O(1) |
| **Binary search on answer** | Monotonic feasibility (min capacity to ship in D days) | O(n log range) |

These show up in **rate limits** (sliding window), **session windows**, and **batching** decisions—not only LeetCode.

### Complexity vs parallelism

[Chapter 06: Concurrency](../06-concurrency-and-multithreading/README.md) does not repeal Big-O: **parallel sort** still has merge cost; **sharded hash map** gives O(1) per shard but cross-shard queries are O(shards). Staff interviews ask: “What breaks when we 10x?”—often **single hot partition** or **superlinear scan**, not “add threads.”

## When to use / when to avoid

**Use structured complexity reasoning when:**

- Sizing a new hot path (search ranking, real-time aggregation, in-process cache).
- Choosing between exact and approximate structures (heap Top-K vs Count-Min Sketch).
- Reviewing a teammate’s PR that nests loops over catalog or user lists.
- Explaining why p99 blew up after a “small” feature added a full scan.

**Avoid when:**

- Dominant cost is network RTT to payment or search—optimize timeouts and caching first ([Ch. 12](../12-caching-strategies/README.md)).
- n is bounded by contract (max 50 line items per cart)—document the bound instead of building a distributed trie.
- Problem is already in the database—push selection and ordering to indexes ([Ch. 10](../10-database-design-and-data-modeling/README.md), [Ch. 11](../11-indexing-and-query-optimization/README.md)).

## How it fails

| Symptom | Likely cause | What to check |
|---------|--------------|---------------|
| CPU linear with catalog growth | O(n) or O(n²) scan per request | Flame graph; log n per request |
| p99 spikes every few minutes | Hash rehash, cache eviction storm, full GC after huge allocation | Heap histogram; map size metrics |
| Memory cliff | Unbounded in-process map (sessions, rate-limit deques per user) | Per-key cardinality; TTL |
| “Works in staging” | Small n hides quadratic | Load test at 10× expected n |
| Sorted-set timeouts in Redis | O(log n) ops × huge fan-out | Key design; sharding |

**Incident pattern:** “Related products” deploy → CPU 3× → rollback. Root cause: in-memory sort of full category. **Fix:** precomputed Top-K, heap streaming, or search index—not faster CPU.

**Debugging hooks:** Compare **p50 vs p99** (tail often allocation or worst-case rehash); **alloc rate** in Go/Java profilers; **operation count** per request in traces.

## Architect takeaway

- **Decide:** Dominant operation and max **n** (per request, per key, per node). Pick structure to match; document amortized vs worst case for SLAs.
- **Measure:** p99 latency, CPU per request, heap per instance, cardinality of keys (maps, deques).
- **Document in design review:** Rejected alternatives (“full sort vs heap Top-K”), bound **n**, eviction/TTL policy for in-memory structures, plan when **n** crosses threshold (shard, push to DB, approximate).

## Diagrams

- [Overview — complexity mental model](./diagrams/overview.md)
- [Structure selection flowchart](./diagrams/structure-selection.md)
- [Time vs space trade-offs](./diagrams/complexity-tradeoffs.md)

## Code examples

| Scenario | Java | Go |
|----------|------|-----|
| LRU session cap | [LruCacheSession.java](./java/LruCacheSession.java) | [lru_cache_session.go](./go/lru_cache_session.go) |
| Top-K trending SKUs | [TopKHotProducts.java](./java/TopKHotProducts.java) | [top_k_hot_products.go](./go/top_k_hot_products.go) |
| Sliding-window rate limit | [SlidingWindowRateLimit.java](./java/SlidingWindowRateLimit.java) | [sliding_window_rate_limit.go](./go/sliding_window_rate_limit.go) |

**Production note:** In-process LRU and sliding windows are correct on **one node** or with **sticky routing**; at fleet scale, prefer Redis/Caffeine/ristretto with explicit TTL, sharded keys, and metrics on eviction rate. Top-K over exact counts may become **streaming sketches** when n is billions of events/day.

## Related topics

- [Chapter 01: SOLID and Core Engineering Principles](../01-solid-and-core-engineering-principles/README.md) — clarity before micro-optimizing structures
- [Chapter 05: Java and Golang Deep Dive](../05-java-and-golang-deep-dive/README.md) — collections, slices, and runtime costs
- [Chapter 06: Concurrency and Multithreading](../06-concurrency-and-multithreading/README.md) — thread-safe maps, lock-free vs complexity
- [Chapter 07: Memory Management](../07-memory-management/README.md) — allocation, GC, locality
- [Chapter 11: Indexing and Query Optimization](../11-indexing-and-query-optimization/README.md) — B-trees and access paths as disk-backed structures
- [Chapter 27: Performance Engineering](../27-performance-engineering/README.md) — profiling validates asymptotic hypotheses
- [Chapter 28: Scalability and Capacity Planning](../28-scalability-and-capacity-planning/README.md) — sharding when single-node structures saturate

## Interview preparation

See [interview-questions.md](./interview-questions.md) (**50** questions). Rationale: core handbook chapter with Staff+ coding-screen weight; user-requested top 50 for drill depth without 100-bank duplication of pure puzzle trivia.
