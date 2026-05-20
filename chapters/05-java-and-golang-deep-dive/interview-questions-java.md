# Interview Questions: Java Deep Dive

**Bank size:** 50  
**Rationale:** Core handbook chapter — dual-stack Staff+ credibility; emphasizes JVM mechanisms, ecosystem trade-offs, coding, and production debugging.  
**Last updated:** 2026-05-20

---

## Foundations

## 1. What does the JVM actually execute, and why does that matter for latency after a deploy?

**Answer:** Java source compiles to **bytecode** run on the JVM. Hot methods are **JIT-compiled** (C1 then C2), so fresh pods see slower p99 until warm. Architects size rollouts and autoscaling with **warmup windows**, use readiness that includes synthetic traffic if needed, and compare p99 pre/post deploy—not just mean CPU.

## 2. Explain the Java memory model in one paragraph a payments engineer would care about.

**Answer:** The JMM defines when writes by one thread are **visible** to another: `volatile`, `synchronized`, and `java.util.concurrent` types establish happens-before edges. Without them, double-checked locking and lazy singletons break. For money movement, prefer **immutable values**, atomic `LongAdder`/`AtomicReference`, or DB constraints—not ad-hoc `volatile` flags alone.

## 3. What is the difference between `==` and `equals` for `Integer` in cache-heavy code?

**Answer:** `==` compares references; for `Integer` outside the **cache range (-128..127)** two equal values can be different objects. Use `equals` for value semantics; for primitives compare directly. Incident pattern: `HashMap` keyed wrong because of autoboxing assumptions in hot paths.

## 4. How do checked and unchecked exceptions shape API design at service boundaries?

**Answer:** **Checked** exceptions force callers to handle or declare—good for recoverable I/O in libraries, noisy in domain layers. **Unchecked** (`RuntimeException`) dominate modern Spring/gRPC code. At HTTP/gRPC edges, **map to stable error codes**; never leak `SQLException` types to clients. Architects standardize: domain throws unchecked + global handler; infrastructure wraps I/O.

## 5. What problem do Java records solve, and when would you still use a class?

**Answer:** **Records** give immutable data carriers with generated `equals`/`hashCode`/accessors—ideal for DTOs, events, command payloads. Use a **class** when you need mutable state, inheritance hierarchies, or complex lifecycle behavior. Records are not entities with JPA lazy loading—keep persistence models separate when ORM behavior matters.

## 6. What are sealed classes/interfaces for in domain modeling?

**Answer:** **Sealed** types restrict which classes can implement/extend—enables exhaustive `switch` and clearer payment state machines (`Authorized`, `Captured`, `Refunded`). Trade-off: framework reflection (some serializers) may need configuration. Use when closed variants are a business rule, not for every DTO.

## 7. Describe generics erasure and one production consequence.

**Answer:** Type parameters are erased at runtime—`List<String>` is a `List` at bytecode. You cannot `new T()`, and `instanceof List<String>` is illegal. Consequences: need **Class<T>** tokens for some frameworks, careful with Jackson/Fastjson type info, and wildcards (`? extends Foo`) for API flexibility. Prefer clear DTO types over raw collections at boundaries.

## 8. What is the JPMS (Java Platform Module System) trying to enforce?

**Answer:** **Modules** export packages explicitly (`module-info.java`), hiding internals and reducing **classpath coupling**. Strong encapsulation breaks illegal reflective access on JDK internals. Brownfield often stays classpath-based; greenfield libraries benefit. Architects use modules or **ArchUnit** when “everything depends on everything.”

## 9. How does `Optional` intend to be used—and how is it misused?

**Answer:** Intended as a **return type** for absent values, not fields/parameters/collections. Misuse: `Optional.get()` without check, serializing Optional in APIs, or avoiding null discipline in entities. In services, prefer explicit empty responses or 404 at the edge; use Optional in domain returns sparingly for clarity.

## 10. What is the difference between `HashMap` and `ConcurrentHashMap` beyond “thread-safe”?

**Answer:** `ConcurrentHashMap` offers **lock-striped** reads/writes, atomic `computeIfAbsent`, and weakly consistent iterators—suited for shared caches. `HashMap` under concurrent mutation can infinite-loop (pre-Java 8) or corrupt. Trade-off: `computeIfAbsent` can still stampede on hot keys—pair with external locking or caching library for thundering herd.

## 11. Explain `ExecutorService` vs “just create threads.”

**Answer:** Pools **bound concurrency**, reuse threads, and offer lifecycle (`shutdown`, `awaitTermination`). Unbounded `new Thread` per task exhausts memory and OS schedulers. Architects pick pool sizes from **downstream limits** (DB connections, partner QPS), not CPU count alone.

## 12. What are virtual threads (Project Loom) and what is pinning?

**Answer:** **Virtual threads** are cheap, JVM-mounted tasks on a small **carrier** platform-thread pool—great for blocking I/O-heavy code without massive platform thread counts. **Pinning** occurs when code holds monitors or does blocking JNI on a carrier, stalling other virtual threads. Measure with JDK pinning diagnostics; avoid `synchronized` around long I/O—use `ReentrantLock` or restructure.

---

## Application

## 13. When would you choose Spring Boot vs a lighter stack (Micronaut, Quarkus, plain JDK)?

**Answer:** **Spring Boot** when team velocity, hiring, and integration breadth dominate—accept startup/memory cost. **Quarkus/Micronaut** for faster startup/native image experiments on Kubernetes. **Plain JDK + jlink** for minimal agents. Decide on **team skill, TTFM, and SLO**—not benchmark heroics alone.

## 14. How do you propagate trace and request IDs through a Java microservice?

**Answer:** Use **OpenTelemetry/Micrometer** with MDC (Mapped Diagnostic Context): ingest `traceparent` at servlet/filter or gRPC interceptor, put `traceId`/`spanId` in MDC, clear MDC in `finally` on thread pools. Virtual threads require **scoped values** or careful MDC propagation—thread-local alone can leak across tasks if pooled incorrectly.

## 15. What defaults in Spring Data JPA often hurt production?

**Answer:** **Open session in view** masks N+1 until load; **lazy loading outside transactions** throws or causes extra queries; default **connection pool** too small; missing **query timeouts**. Architects disable OSIV for APIs, use `@Transactional` boundaries explicitly, tune pool = expected concurrent requests × avg query time / target latency.

## 16. How does gRPC on Java differ from REST for an internal payment rail?

**Answer:** gRPC gives **binary protobuf**, HTTP/2 multiplexing, streaming, and strong contracts—good east-west at ~1–10k+ RPS per instance with codegen. REST wins for browsers, public partners, caching. Java stubs are thread-safe; set **deadlines** on stubs. Map `Status.Code` to retry policy—`UNAVAILABLE` vs `INVALID_ARGUMENT`.

## 17. Explain JDBC connection pool sizing at order-of-magnitude.

**Answer:** Rule of thumb: pool size ≈ **(core_count × 2) + effective_spindle** is outdated for SSD and virtual threads. Better: `connections = (peak concurrent requests holding DB) + margin`, capped by **DB max_connections / num_instances**. Too large pools → DB contention; too small → queueing in app threads. Watch **pool wait time** metric, not just active count.

## 18. What is Resilience4j used for compared to Hystrix?

**Answer:** **Resilience4j** provides circuit breaker, rate limiter, bulkhead, retry with functional composition—maintained successor spirit to Hystrix. Use to **isolate** fraud, PSP, or catalog calls. Architects define **fallback semantics** (degrade vs fail) per dependency SLI, not blanket retries.

## 19. How do you structure multi-module Maven/Gradle for a modular monolith?

**Answer:** Modules: `domain` (no Spring), `application` (use cases), `adapters` (infra), `boot` (main). Enforce **dependency direction** with ArchUnit: domain never imports JDBC. Version alignment via BOM; one deployable initially. Extract microservice only when scaling/ownership forces it.

## 20. What is the role of `CompletableFuture` in modern Java services?

**Answer:** Composes **async pipelines** without callback hell—`thenCombine`, `orTimeout`, `handle`. Use with **custom executor** for CPU work; default `ForkJoinPool.commonPool()` is shared and can starve. Pair timeouts with **cancellation** of underlying HTTP calls where client supports it.

## 21. How does Jackson behave differently from Gson for API evolution?

**Answer:** Jackson supports **annotations, mixins, polymorphic types**—powerful but footgun (`@JsonTypeInfo`). Unknown fields: `FAIL_ON_UNKNOWN_PROPERTIES` vs ignore for forward compatibility. Architects document **versioned DTOs** and compatibility tests—don’t rely on silent ignore without policy.

## 22. What is GraalVM native image trade-off for Java microservices?

**Answer:** **Pros:** fast startup, lower RSS, good for scale-to-zero. **Cons:** build complexity, reflection configuration, longer CI, some libraries incompatible. Use for **edge/lambda/sidecar** workloads; stick to JVM for heavy Spring + dynamic classpath unless team invests in native CI.

---

## Coding

## 23. Implement thread-safe lazy initialization without double-checked locking bugs.

**Answer:** Use **holder idiom** (`static final` nested class), `enum` singleton, or `synchronized` factory. If DCL, make field `volatile`. Example pattern: `private static class Holder { static final Service INSTANCE = new Service(); }`. Prefer simplicity over micro-optimization unless profiling proves need.

## 24. Write a method to group orders by merchantId with stable ordering.

**Answer:** `orders.stream().collect(Collectors.groupingBy(Order::merchantId, LinkedHashMap::new, Collectors.toList()))` preserves insertion order per key if source ordered. For large data, consider **database GROUP BY**—don’t load millions into heap. Complexity O(n) time, O(keys) memory.

## 25. How do you cancel a long-running task submitted to an executor when the HTTP client disconnects?

**Answer:** Keep `Future<?>`; on servlet async timeout or cancellation callback, call `future.cancel(true)` if interruptible; ensure tasks check `Thread.interrupted()` and close resources. With virtual threads, **interrupt** closes blocking I/O on many JDK APIs. Propagate cancellation to downstream HTTP clients via request context.

## 26. Parse a CSV of 10M rows in Java without OOM—sketch approach.

**Answer:** Stream with **BufferedReader** line-by-line or use univocity/commons-csv iterator; batch insert JDBC **batch updates** (1k rows); don’t `readAll()`. Backpressure: pause read if DB slow. Parallelize only after proving safe partition; mind connection pool.

## 27. Find duplicate idempotency keys in a `List<String>` efficiently.

**Answer:** `Set<String> seen = new HashSet<>();` single pass O(n) time O(n) space. For disk-scale, external sort or DB `GROUP BY key HAVING COUNT(*) > 1`. Return first duplicate for alerting.

## 28. Implement a simple per-merchant rate limiter (token bucket) in Java.

**Answer:** `ConcurrentHashMap<String, RateLimiter>` (Guava) or Resilience4j limiter per key with eviction policy for unbounded merchants. Cap map size + TTL to prevent memory leak. Return 429 when `tryAcquire` fails.

## 29. Reverse a linked list iteratively—why do interviewers still ask this in Java loops?

**Answer:** Tests pointer discipline—`prev`, `curr`, `next` without losing references. O(n) time O(1) space. Staff angle: same care applies to **iterator invalidation** and custom lock-free structures—know when to use `Collections` instead.

## 30. Merge two sorted lists of `OrderEvent` by timestamp for audit replay.

**Answer:** Two-pointer merge O(n+m). If lists are on disk, **external merge** like merge phase of mergesort. Use `Comparator` consistent with equals; tie-break on event ID for stability.

## 31. How would you implement a circuit breaker without a library (sketch)?

**Answer:** States CLOSED/OPEN/HALF_OPEN; count failures in window; OPEN fails fast; after cooldown allow probe success to CLOSE. Thread-safe counters (`AtomicInteger`), clock injectable for tests. Production: use Resilience4j with metrics export.

## 32. Write a comparator for `Transaction` that sorts by amount desc, then time asc.

**Answer:** `Comparator.comparing(Transaction::amount).reversed().thenComparing(Transaction::time)` or `Comparator.comparing(Transaction::amount, Comparator.reverseOrder()).thenComparing(Transaction::time)`. Null-safe variants with `nullsLast` if nullable fields exist.

---

## Design & Trade-offs

## 33. Java service calls Go service—how do you design the contract?

**Answer:** **Protobuf/gRPC** or OpenAPI with generated clients; shared **error model** (retryable codes); propagate **W3C trace context**; agree on **idempotency-key** header and timeout budgets. Avoid Java-specific serialization (Java serialization) across boundary.

## 34. Immutable domain objects vs JPA entities—where do you draw the line?

**Answer:** **Entities** map tables, manage lazy loads and flushing—mutable by necessity. **Domain records** express invariants for business logic; map between layers (mapper or manual). Don’t leak Hibernate proxies into event publishers—detach or use DTOs.

## 35. When is `synchronized` acceptable vs `ReentrantLock`?

**Answer:** `synchronized` is simpler and JVM-optimized for **short critical sections**. `ReentrantLock` gives tryLock, fairness, and **doesn’t pin virtual threads** the same way in some JDK builds—prefer for I/O-adjacent caution. Neither replaces DB transactions for money consistency.

## 36. Should you use reactive (WebFlux) for a new checkout API?

**Answer:** Choose reactive when team masters it **and** workload is I/O multiplexed end-to-end (including JDBC—R2DBC). Otherwise **blocking + virtual threads** often beats partial reactive with blocking JDBC on event loop. Measure complexity cost vs throughput gain.

## 37. How do you version a public Java library used by 20 internal teams?

**Answer:** **Semantic versioning**, binary compatibility checks (japicmp), deprecation cycles, and changelog. Avoid breaking `public` APIs without major bump. Provide migration guides; use **consumer-driven contract tests** for critical adopters.

## 38. Monolith on Java 17 vs microservices—decision frame?

**Answer:** Start modular monolith if one team and unified deploy. Split when **independent scaling, failure isolation, or ownership** clear—and APIs stable. Java 17+ gives records, sealed, improved GC—doesn’t mandate microservices.

## 39. Lombok: productivity boost or review hazard?

**Answer:** Reduces boilerplate (`@Data`, builders) but hides **equals/hashCode** semantics and can break with field additions. Architects allow with rules: no `@Data` on entities, explicit `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`, code review for API surface.

## 40. How do you choose heap size for a 4 vCPU container running Spring?

**Answer:** Container limit minus **metaspace, thread stacks, native, direct buffers**. Heap often 50–70% of limit for JVM 17 with G1/ZGC. Load test p99 GC pause; avoid heap = container limit (OOMKill). Document `-XX:MaxRAMPercentage`.

## 41. Event publishing from `@Transactional` service—pitfalls?

**Answer:** **Outbox pattern** avoids dual-write: insert outbox in same TX, separate publisher reads. `@TransactionalEventListener` helps after-commit but doesn’t replace outbox if broker down. Never publish before commit unless idempotent consumers tolerate duplicates.

## 42. Java vs Go for a new API gateway—staff-level comparison?

**Answer:** Go: small image, fast boot, stdlib HTTP, easy static deploy. Java: richer filter ecosystems, mature auth integrations, team skill. Gateway bottleneck is often **network and TLS**, not language—pick ops maturity and hiring. Both need timeout, rate limit, and observability parity.

---

## Debugging & Ops

## 43. p99 latency doubled after deploy—Java-specific checklist?

**Answer:** Check **GC logs** (pause, allocation rate), new pod **JIT warmup**, changed **pool sizes**, increased logging, thread pool queue depth, **DB wait**, and dependency timeouts. Compare canary vs old version with same load shape.

## 44. How do you capture a heap dump safely in production?

**Answer:** `jcmd <pid> GC.heap_dump`, or Kubernetes **ephemeral debug container** with same image. Dump to volume with space; compress; restrict access (PII). Prefer **low-traffic window**; heap dump pauses can be seconds—use concurrent collectors and test procedure.

## 45. `OutOfMemoryError: Metaspace`—what now?

**Answer:** Classloader leak (hot redeploy without restart), excessive dynamic proxies, or too many generated classes. Analyze histogram of loaded classes; fix leak (undeploy path), increase `-XX:MaxMetaspaceSize` temporarily, reduce dynamic codegen. Restart buys time—not root fix.

## 46. Thread dump shows thousands of `BLOCKED` threads—interpretation?

**Answer:** Find **lock owner** stack; often DB pool exhaustion, synchronized bottleneck, or external service. Correlate with pool metrics and slow query log. Fix by shrinking critical sections, increasing pool only if DB allows, or caching.

---

## Staff+

## 47. Two teams want different Java versions (11 vs 21)—what do you standardize?

**Answer:** Platform team publishes **supported LTS ladder** with sunset dates; exceptions need risk acceptance. JDK 21 brings virtual threads and pattern matching—justify delay vs security patches. CI enforces version; containers use one base image family.

## 48. How do you review a junior’s PR that uses `parallelStream()` on a shared list?

**Answer:** Flag **thread-safety** and **ForkJoinPool** side effects; common pool contends with other work. Prefer explicit executor with bounds or sequential unless data is huge and profiling proves benefit. Require test under load.

## 49. Principal scope: org runs 400 Java services—what three platform investments?

**Answer:** Example: (1) **standard observability** (OTel, SLO templates), (2) **safe dependency bot** with CVE SLA and compatibility CI, (3) **golden paths** (service template, arch tests, rollout checks). Measure adoption % and incident MTTR—not lines of platform code.

## 50. You must defend keeping Java for core ledger while Go ships edges—one paragraph for execs.

**Answer:** Ledger needs **mature transactional tooling, audit integrations, and deep hiring pool**; migration risk exceeds benefit. Go edges win **cost per instance and speed of shipping stateless gateways**. Shared contracts and observability keep the system one platform; language split follows **bounded context ownership**, not fashion.
