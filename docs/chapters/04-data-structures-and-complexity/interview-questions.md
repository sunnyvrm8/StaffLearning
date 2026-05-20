# Interview Questions: Data Structures and Complexity

**Bank size:** 50  
**Rationale:** Core Phase A chapter; Staff+ coding screens and complexity literacy for performance chapters—user-requested top 50 with coding and trade-off emphasis.  
**Last updated:** 2026-05-20

---

## Foundations

## 1. What is the difference between O, Ω, and Θ notation? When does an architect care in a design review?

**Answer:** **O** is an upper bound (worst-case growth rate), **Ω** a lower bound, **Θ** a tight bound when both match. In reviews you usually state **Θ or O for the dominant term** on the hot path—e.g., “per-request scan is O(catalog size).” Architects care when **n can 10×** (SKU count, active users, graph edges): a loose O(n) bound that hides O(n²) nested loops is a capacity risk. You rarely prove Ω in production docs unless arguing a **lower bound is unavoidable** (comparison sort Ω(n log n)).

---

## 2. Explain amortized analysis with one production example.

**Answer:** **Amortized** averages expensive rare operations over many cheap ones. Dynamic array append is **amortized O(1)** but a single resize is **O(n)** copy. Hash map insert is usually O(1) but **rehash** can spike latency when load factor crosses threshold. For SLAs, call out **worst single-op latency** (p99 during rehash) vs **average**—payments teams have missed this when a bulk import triggered map growth on the request thread.

---

## 3. When is O(n) better than O(log n) in practice?

**Answer:** When **n is tiny and constants dominate**: n ≤ 32, contiguous array scan beats tree pointer chasing due to **cache locality**. Also when data is **already in memory sequentially** and you need full scan anyway—adding a tree adds indirection without skipping work. Rule: profile; Big-O crosses over at some n₀ where log factors and cache misses matter.

---

## 4. What is a hash collision and how do open addressing and chaining differ?

**Answer:** Two keys map to the same bucket. **Chaining** stores colliding entries in a list/tree at the bucket (Java `HashMap` buckets). **Open addressing** probes alternate slots in the table. Both degrade as load factor rises. Production: bound map size, monitor **probe depth / chain length**, choose **good hash** for untrusted keys (avoid algorithmic complexity attacks). Distributed analog: **hot partition** is a “collision” at shard level.

---

## 5. Why do balanced trees exist if hash maps are O(1)?

**Answer:** Hash maps give **expected** O(1) without **ordering**. You need **O(log n) ordered operations**—range scan, successor, floor/ceiling—for scheduling, time-series windows, or in-memory indexes. **B-trees** extend the idea to disk block sizes ([Ch. 11](../11-indexing-and-query-optimization/README.md)). Pick hash when you only point-lookup; pick tree when **range or sort order** is on the hot path.

---

## 6. What problem does a min-heap solve that a sorted array does not?

**Answer:** **Dynamic** min/max: insert and extract in **O(log n)** without resorting the whole array (**O(n log n)**). A sorted array has O(1) min if ascending but **O(n)** insert. Heaps power **Top-K**, schedulers, and merge-k-streams. Trade-off: no efficient arbitrary search; not a replacement for a full index.

---

## 7. Define BFS and DFS; name one production use each.

**Answer:** **BFS** explores layer by layer with a queue—**shortest path in unweighted graphs**, propagate level-by-level (permissions, CDN cache invalidation tiers). **DFS** goes deep first with stack/recursion—**cycle detection**, topological order for build pipelines, maze-like dependency resolution. Both are **O(V + E)**; choice is **which answer** you need first, not raw speed.

---

## 8. What is Union-Find and when would you use it in a backend service?

**Answer:** **Disjoint-set** structure supporting **union** and **find** in nearly constant amortized time. Use when edges arrive incrementally and you need **connected components**—fraud account linking, network reachability, merging duplicate user records. Avoid O(n²) “compare every pair” when millions of edges stream in nightly.

---

## 9. What is the difference between average-case and worst-case for hash table lookup?

**Answer:** **Average** (with simple uniform hashing assumption): O(1). **Worst**: O(n) if all keys collide or an attacker crafts collisions. Production APIs facing untrusted input use **salted hashes**, treeified buckets (Java 8+), or limits. Capacity planning uses **average**; **SLO and security** care about **worst** and adversarial cases.

---

## 10. What does “space complexity” buy you on a hot path?

**Answer:** Extra space can **buy time**: hash map for O(1) lookups, prefix sums for O(1) range queries, auxiliary heap for Top-K. Trade-off: **heap memory**, GC pressure, eviction complexity. When memory is bounded (mobile edge, Lambda), you may choose **streaming approximations** (Count-Min) over exact O(n) structures.

---

## Application

## 11. You need “recently viewed products” per user with a max of 20 items. Which structure and complexity?

**Answer:** **Hash map userId → bounded list** or **LinkedHashMap / list + map** for O(1) touch and eviction of oldest. Per update: O(1) amortized. Store **product IDs**, not full product blobs. At fleet scale, move to **Redis LIST** with LTRIM or per-user capped stream—same logic, distributed TTL.

---

## 12. How would you implement a sliding-window rate limiter in memory?

**Answer:** Per key, keep a **queue/deque of timestamps**; on each request, **drop timestamps older than window**, then reject if count ≥ limit else append now—**O(events in window)** per check, bounded by limit. Alternative: **token bucket** O(1) but burstier. Distributed: Redis **sorted set** of timestamps or atomic sliding scripts; watch **memory per active user**.

---

## 13. “Trending products” needs top 10 by view count from 1M SKUs updated every minute. Approach?

**Answer:** Increment counts in **hash map** O(1) per event; each minute extract Top-10 with **min-heap size 10** over map entries—O(n log 10) ≈ O(n). Avoid full sort O(n log n) on every tick if n is huge; for very large n use **approximate** heavy-hitters or pre-aggregate in stream processor (Flink) with keyed state.

---

## 14. When is a trie worth it over a hash map for search suggestions?

**Answer:** When queries are **prefix-based** (“lap” → laptop, lap desk) and you need **ordered suggestions** under a prefix. Trie: O(L) per query for key length L. Hash map needs **scan all keys** O(n) unless you maintain a separate index. Cost: trie memory and update path on catalog churn; often delegated to **Elasticsearch completion** or dedicated suggest index.

---

## 15. How do prefix sums help billing or analytics range queries?

**Answer:** Precompute **prefix[i] = sum(arr[0..i])** in O(n). Range sum [l,r] = prefix[r] - prefix[l-1] in **O(1)** after build. Use for **daily usage buckets**, score windows, or immutable arrays with many range queries. If values change often, use **Fenwick tree / segment tree** for O(log n) updates.

---

## 16. Two-pointer technique: when does it apply on unsorted data?

**Answer:** Typically **sorted** input (pair sum, container with most water, merge two sorted lists). On unsorted data, two pointers can still work for **in-place partition** (quickselect) or **linked list** cycle detection (slow/fast). If unsorted and need pairs, hash map is usually O(n) time O(n) space.

---

## 17. How would you detect a cycle in a workflow dependency graph?

**Answer:** **DFS with recursion stack** or **Kahn’s topological sort** (BFS on indegree). If topological sort cannot include all nodes, cycle exists. Complexity O(V+E). Production: fail CI on cyclic DAG; runtime guard before executing workflow.

---

## 18. What structure backs LRU cache semantics and what are the complexities?

**Answer:** **Hash map + doubly linked list** (or LinkedHashMap access-order): get/put **O(1)** with eviction of tail when over capacity. **Trade-off:** not thread-safe without locking; **distributed LRU** needs TTL + approximate policies (Redis `maxmemory` policies). See chapter snippets `LruCacheSession`.

---

## Design & Trade-offs

## 19. Exact Top-K vs approximate Count-Min Sketch for trending—trade-off?

**Answer:** **Exact heap/map**: correct ranks, O(n) or O(n log K) work, heavy memory on huge cardinalities. **Count-Min Sketch**: sublinear memory, **probabilistic** ranks, good for “heavy hitters” at billions of events/day. Choose exact when product needs **deterministic** leaderboard; sketch when **1% error** is acceptable and ops cost dominates.

---

## 20. In-process LRU vs distributed cache (Redis)—when to switch?

**Answer:** **In-process** when latency budget &lt; 1 ms, data is **node-local** or stickiness OK, and size is bounded (MB–low GB). **Redis** when **multiple instances** need shared state, **larger working set**, or survival across deploys. Cost: network RTT + serialization. Hybrid: local LRU fronting Redis ([Ch. 12](../12-caching-strategies/README.md)).

---

## 21. Array vs linked list for a work queue in a high-throughput worker pool?

**Answer:** Prefer **bounded ring buffer** (array) for **cache locality** and predictable allocation; linked lists add pointer overhead and GC churn. Go/Java bounded channels often sit on array-backed queues. Linked list wins only if you need **unbounded** splice with stable iterators—rare in request workers.

---

## 22. Why might a database index be a B+ tree instead of a hash index?

**Answer:** **Range queries** (`WHERE created_at BETWEEN`) and **ordered scans** need leaf-linked B+ trees. Hash index only helps **equality**; no ordering. Most OLTP composite indexes are B-tree family. Architect links storage structure to **query shape**, not “hash is faster.”

---

## 23. O(n log n) sort per request vs precomputed Top-K table—how do you decide?

**Answer:** If **read:write ≫ 1** and catalog changes slowly, **precompute** (batch job, materialized view, search index). If catalog tiny (&lt;200 items), sort per request is fine—document bound. Break-even: when **sort CPU** appears in flame graphs at target QPS (often 500+ QPS with n &gt; 5k).

---

## 24. Sharding a hash map by userId—what complexity breaks?

**Answer:** Single-key ops stay **O(1) per shard**. **Cross-shard** queries (global leaderboard, “all users in org”) become **O(shards)** or need **secondary index**. **Hot key** makes one shard O(all traffic)—complexity is “O(1) per op” but **skew** violates capacity model ([Ch. 28](../28-scalability-and-capacity-planning/README.md)).

---

## 25. Fixed-window vs sliding-window rate limits—complexity and fairness?

**Answer:** **Fixed window**: O(1) counter per window bucket; **burst at boundary** (2× limit in 1 second straddling windows). **Sliding window**: fairer, **O(window count)** memory per key or approximate sliding log. Token bucket: O(1), smooths bursts, different semantics. Pick by **fairness** requirement, not only Big-O.

---

## 26. When would you accept O(n²) in production code?

**Answer:** When **n is hard-capped** by contract (n ≤ 100), offline batch with SLA in minutes, or one-time migration. Document the cap in API limits. Never silent O(n²) on **unbounded user input**—that becomes incident at scale.

---

## 27. Graph: store adjacency list vs matrix for 10M nodes, sparse edges?

**Answer:** **Adjacency list**: O(V+E) space, good for sparse social/fraud graphs. **Matrix**: O(V²) impossible at 10M nodes. List wins unless dense clique (rare). Use compressed formats or graph DB when edges don’t fit one machine.

---

## 28. Compare stack, queue, and deque for undo/redo and task scheduling.

**Answer:** **Stack** LIFO: undo/redo, DFS, syntax parsing. **Queue** FIFO: fair scheduling, BFS. **Deque** both ends: sliding window timestamps, work-stealing ends. Complexity: O(1) push/pop at used end; array deque may resize amortized O(n).

---

## Coding

## 29. Given an array of integers, return indices of two numbers that sum to target. Expected approach and complexity?

**Answer:** One pass with **hash map value → index**: for each x, check if `target - x` exists—**O(n)** time, **O(n)** space. Sort + two pointers is O(n log n) if you cannot use extra space and may need to return values not indices. State duplicate handling and integer overflow if asked.

---

## 30. Find the longest substring without repeating characters.

**Answer:** **Sliding window** with hash set/map of last index: expand right, shrink left while duplicate—**O(n)** time, O(min(n, alphabet)) space. Production variant: bounded alphabet (ASCII) vs Unicode (map size). Tie to session or token parsing windows.

---

## 31. Merge k sorted lists of total length n. Complexity target?

**Answer:** **Min-heap of size k**: pop smallest, push from that list—**O(n log k)**. Better than merge pairs O(n log k) repeated poorly. If k=2, two-pointer merge O(n). Link to **log aggregation** merging sorted shard outputs.

---

## 32. Implement LRU cache with get and put in O(1). What breaks under concurrency?

**Answer:** Map key → list node + doubly linked list for order; on get, move to head; on put, evict tail if over capacity. **Concurrency:** race on list pointers—need **segmented locks**, `ConcurrentHashMap` + synchronized list, or external cache library. **False sharing** if locking per node—shard by key hash.

---

## 33. Top K frequent elements in a stream. Space/time?

**Answer:** **Hash map counts** + **min-heap size k** → O(n log k) time, O(n) space for distinct keys; or **bucket sort** by frequency if counts bounded. Streaming: heap over sliding window map with expiry. Mention **Quickselect** average O(n) for one-shot if only k needed once.

---

## 34. Detect cycle in a linked list. Follow-up for cycle start?

**Answer:** **Floyd slow/fast** pointers for cycle detection O(n), O(1) space. To find start: reset one pointer to head, advance both one step—meeting point is start. Production analog: **poison message** loops in retry graphs.

---

## 35. Binary search: when is it valid on a rotated sorted array?

**Answer:** When sequence is **sorted with a single pivot**—compare mid to bounds to decide which half is sorted, recurse O(log n). Requires **total ordering**. Not valid on “sorted by popularity” without explicit monotonic key.

---

## 36. Number of islands in a grid—approach and complexity?

**Answer:** **DFS/BFS** each unseen `'1'`, mark visited—**O(rows × cols)** time and O(rows×cols) worst recursion/stack space. Production: flood-fill connected components on feature flags, region outages, or image tiles—bound stack or use iterative BFS for deep grids.

---

## 37. Implement a queue using two stacks. Amortized complexity?

**Answer:** **In-stack** push, **out-stack** pop; when out empty, pour in—each element moved once → **amortized O(1)** per op. Worst single pop can be O(n). Useful when API exposes only stack but need FIFO semantics in one thread.

---

## 38. Given logs with (userId, timestamp), count users active in last 5 minutes—structure?

**Answer:** Per event or periodic sweep: **hash map userId → last seen**; prune stale entries or use **deque of events** in sliding window. Complexity O(events) per window refresh. At scale: **streaming keyed state** with window triggers, not global scan of all users.

---

## System Design

## 39. Design an in-memory “nearby drivers” index for ride-hailing at 50k online drivers. What structures?

**Answer:** **Geospatial index**: grid/cell hash (geohash bucket) or quadtree/R-tree for O(log n + k) neighbor queries—not linear scan O(n). Partition by **city/cell**; replicate hot cities. Complexity per query depends on **k neighbors** and cell density. Back with Redis GEO or dedicated engine; document **staleness** and refresh rate.

---

## 40. You must rank 10M products by score every second for a homepage. Bottleneck and fix?

**Answer:** Bottleneck: **full sort O(n log n)** per second on 10M rows. Fixes: **incremental Top-K heap** on changed scores only, **precomputed** shard leaders merged with heap of shard tops (O(shards log shards)), or **search index** with sorted field. Capacity: ~10M × 8 bytes read ≈ 80 MB/s minimum memory bandwidth before CPU—numbers justify approximate or partitioned ranking.

---

## 41. Idempotency key store for payments—structure and TTL?

**Answer:** **Hash map** (or DB unique index) key → response metadata, **TTL ≥ retry window** (24–72h). O(1) lookup on hot path. Complexity at scale: **shard by key hash**, watch **memory**; don’t O(n) scan keys. Tie to [Ch. 23](../23-idempotency-sagas-and-distributed-transactions/README.md).

---

## 42. Autocomplete for 50M product titles—single service, p99 &lt; 50 ms. Outline.

**Answer:** **Offline-built trie** or compressed DAFSA in memory + rank bucket per prefix, or delegate to **search cluster** with edge cache. Updates via async rebuild or dual-buffer swap. Avoid SQL `LIKE 'prefix%'` without index on each keystroke. QPS estimate: 20k autocomplete × 5 chars ≈ 100k lookups/s—must be O(prefix length) or cached prefix results.

---

## 43. Detect duplicate transactions in a 1-hour window per card. Complexity at 100k TPS?

**Answer:** **Keyed state**: hash map cardId → deque/set of txn ids or amounts in window, prune on ingest—O(1) per event amortized with bounded window size. At 100k TPS, **partition by card hash**, aggregate in stream processor; store in **Redis ZSET** with timestamp score for sliding window. Global pairwise compare is impossible O(n²).

---

## 44. Design a leaderboard for a game with 10M players, updates every 100 ms.

**Answer:** **Shard** by league/region; per shard **sorted set** (Redis ZSET) O(log n) update. Global top 100: merge **shard tops** with heap size 100 × shards—O(shards log 100). Full global sort each tick is O(n log n)—reject. Accept **eventual** global view by 100–500 ms if cross-shard merge async.

---

## Debugging & Ops

## 45. CPU doubled after deploy; flame graph shows `Arrays.sort` in request path. Next steps?

**Answer:** Confirm **input size n** per request (metrics/log sample). Compare to pre-deploy feature flag. Fix: **Top-K heap**, precomputed index, or push sort to async job—not bigger pods. Add **assertion or metric** when n &gt; threshold. Regression test with production-sized catalog in load test.

---

## 46. Heap OOM on one pod; others fine. What data-structure smells?

**Answer:** **Unbounded in-memory map** (sessions, rate-limit deques, dedup without TTL), **memory leak** holding references, or **hot key** shard on one pod. Heap dump: dominator tree for largest `HashMap`/`[]byte`. Fix: cap size, TTL, move to Redis, or **consistent hash** traffic. Link [Ch. 07](../07-memory-management/README.md).

---

## 47. p99 latency spikes every ~5 minutes on API nodes. Could algorithmic causes include hash tables?

**Answer:** Yes: **periodic rehash**, **cache full eviction storm**, **GC** after large young-gen promotion from batch append, or **log rotation** coinciding—not only “network blip.” Correlate spikes with **map size**, **GC logs**, cron. Profile one spike window with **async profiler**.

---

## Staff+

## 48. A junior engineer proposes a trie in the payment service for 200 merchant configs. Your review?

**Answer:** **Reject complexity**: n=200, static config—**hash map or sorted list** loaded at startup is O(1) or O(log n) with simpler ops. Trie adds memory and bug surface without prefix-search requirement. Teach **bound n** and match structure to **operation mix**; document in PR comment for learning, not shame.

---

## 49. How do you teach complexity to a team that only profiles after incidents?

**Answer:** Add **complexity line to RFC template** (dominant op, n, bound). Run **one load test** before launch with 10× n. Pair postmortems with **“what was asymptotic class?”** not only “scale up.” Office hours with **two real graphs** (good vs nested loop). Tie to **error budget** so perf regressions are visible in SLO dashboards ([Ch. 24](../24-reliability-engineering/README.md)).

---

## 50. Principal loop: “We skipped DS&A study—is that OK for architect-only interviews?”

**Answer:** **Architect-only** loops may skip live coding but still test **complexity reasoning** in system design (sharding, hot keys, ranking). Risk: you cannot spot O(n²) in review or challenge vendor claims. Recommendation from Plan.md: **skim Ch. 4** if strong, but drill **5–10 coding questions** on heaps, hash maps, and sliding window so whiteboard trade-offs are fluent. Weak spot shows when designing **real-time ranking** or **rate limits** under pressure.
