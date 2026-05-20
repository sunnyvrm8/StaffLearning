# Interview Questions: Memory Management (JVM + Go GC, Allocation, Leaks)

**Top 10** with answers — latency tails, OOM incidents, and runtime tuning at architect depth.  
**Last updated:** 2026-05-20

---

## Core

## 1. A checkout service runs fine at p50 but p99 spikes every few seconds—name three memory-related causes before blaming the database.

**Answer:** **Stop-the-world GC** (young or mixed collections) pauses the JVM while mutator threads wait—visible as periodic latency stairs on p99, often correlating with **allocation rate** not heap “full.” **Promotion / old-gen pressure** when short-lived request objects survive a young collection and fill the old generation, triggering longer mixed or full GCs. **Off-heap or native memory** (Netty direct buffers, gRPC, compression dictionaries) that does not show in heap graphs but still competes for **container RAM** and can trigger **OOMKilled** without a Java heap dump. In Go, similar tails come from **GC cycles**, **STW assists** under allocation pressure, or **goroutine/stack growth**—check `GOGC`, allocation rate, and whether p99 aligns with `runtime: gc` in logs before tuning SQL.

---

## 2. Contrast JVM generational GC (G1) with Go’s concurrent collector in one paragraph a platform team would use in a design review.

**Answer:** **G1** partitions the heap into regions, collects **young** objects often (cheap) and **mixed/old** less often (expensive), with a **pause-time goal** (`MaxGCPauseMillis`) traded against throughput. The JVM assumes **most objects die young**—tuning is about **heap size, region size, and allocation rate**. **Go’s GC** is **non-generational** (for the most part): it traces from roots with a **write barrier**, runs **concurrently** with mutators, and is driven by **`GOGC`** (heap growth trigger) rather than explicit young/old sizing. **Trade-off:** JVM gives mature **pause predictability knobs** and rich tooling (`jcmd`, JFR); Go favors **simplicity and low ops surface** but less fine-grained pause targeting. Neither removes the law: **more allocations per request → more GC work**—fix hot paths before buying bigger pods.

---

## 3. What is allocation rate, and why is “we only use 40% of heap” a weak argument against GC pain?

**Answer:** **Allocation rate** is bytes (or objects) allocated per second on the hot path—often **orders of magnitude** more important than **steady-state heap utilization**. A service at 40% heap can still allocate **hundreds of MB/s** of short-lived garbage (JSON parsing, `String` concatenation, defensive copies), forcing **frequent young collections** and **p99 tails** even without OOM. Low utilization with high churn means GC is **busy, not idle**. Measure with **JFR Allocation Profiling**, async-profiler `alloc`, or Go `runtime.ReadMemStats` / `pprof -alloc_space`. Architect action: set SLO on **p99 under load**, correlate with **GC pause metrics** (`jvm.gc.pause`, `go_gc_duration_seconds`), and reduce allocations (reuse buffers, streaming parsers) before raising `-Xmx`.

---

## 4. When would you choose ZGC or Shenandoah over G1 for a Java payments API—and when would you stay on G1?

**Answer:** Choose **ZGC / Shenandoah** when **sub-10 ms p99 pause** on **multi-GB heaps** is an explicit SLO (large in-memory caches, heavy session state, analytics adjacency) and you can accept **throughput trade-offs** and newer JDK operational maturity. Stay on **G1** when the fleet is on **LTS JDK 11/17**, teams know G1 flags, heap is **modest (<8–16 GB)**, and pain is really **allocation churn**—a faster collector on a leaky path only **moves** the cliff. **Avoid** switching collectors during an incident without evidence: compare **pause percentiles vs allocation rate** first. Document: JDK version, heap cap, pause SLO, and rollback plan—collector choice is a **capacity + latency** decision, not a benchmark trophy.

---

## 5. Explain `GOGC`, heap goal, and why lowering it can *increase* CPU and latency.

**Answer:** **`GOGC=100`** (default) triggers a GC cycle when live heap grows to ~100% of the size after the last GC—higher values (**200, 300**) let the heap grow more between cycles → **fewer GCs**, more RAM, often **higher throughput** but **larger live sets** and riskier OOM near cgroup limits. **Lower GOGC** (e.g., 50) collects **more often** → lower peak heap, sometimes **smoother** memory under tight containers, but **more CPU in GC** and can **worsen** p99 if the service is already CPU-bound. **`GOMEMLIMIT`** (Go 1.19+) caps process memory and makes the runtime **aggressively GC** as the limit approaches—good for Kubernetes safety, bad if set without headroom for stacks and off-heap. Tune from **prod load tests**: memory limit, GOGC/GOMEMLIMIT, and **p99 + CPU** together—not heap % alone.

---

## 6. Your pod hits OOMKilled at 2 GiB but the JVM reports heap max 1.5 GiB—what else consumes memory?

**Answer:** **Native / metaspace / thread stacks:** each thread ~1 MB stack by default on HotSpot; **10k threads** is gigabytes before heap. **Metaspace** for classes (especially dynamic proxies, Groovy, many class loaders). **Direct buffers** (`ByteBuffer.allocateDirect`, Netty) live **outside** heap—NIO and gRPC can hold **hundreds of MB** per instance. **JNI, JIT code cache, compressed class space, malloc arenas.** In containers, **`-Xmx` must leave headroom** for non-heap: rule of thumb **heap ≤ 70–75% of cgroup limit** on Java 11+ with direct I/O; validate with **Native Memory Tracking** (`-XX:NativeMemoryTracking=summary`) in staging. Go binaries: watch **heap + stack + span overhead**; `GOMEMLIMIT` should be **below** cgroup limit with margin. Symptom: heap dumps look “fine” while kube still kills the pod—always chart **RSS vs heap**.

---

## Stretch

## 7. Walk through diagnosing a suspected memory leak in production without taking the service down for hours.

**Answer:** **Confirm leak vs churn:** RSS or container memory **monotonic climb over 24h+** after traffic plateaus, not spike-and-plateau per deploy. **JVM:** enable **heap dump on OOM** (`-XX:+HeapDumpOnOutOfMemoryError`), capture **periodic dumps** from one canary during low traffic (`jcmd <pid> GC.heap_dump`), compare **dominator tree** in MAT/VisualVM—look for giant `HashMap`, `byte[]`, `ConcurrentHashMap`, class loaders. **Go:** `curl :6060/debug/pprof/heap` (with auth), `go tool pprof -base=old.heap new.heap`, inspect **inuse_space** growth; check **goroutine** profile for leaks (`pprof/goroutine`). **Metrics:** `jvm.memory.used`, process RSS, GC time ratio, **old-gen** growth after full GC. **Safe mitigations:** rolling restart with **fixed image** while fixing root cause; cap unbounded caches with **TTL/size**; never disable GC in prod. Tie leaks to code patterns: static maps, `ThreadLocal` without remove, listeners, unbounded queues ([Ch. 06](../06-concurrency-and-multithreading/README.md)).

---

## 8. Name four production leak or “effective leak” patterns in Java and one mitigation each.

**Answer:** **Unbounded in-memory caches** (sessions, idempotency, rate-limit deques)—mitigate with **TTL, max entries, Caffeine `maximumSize`**, or Redis. **`ThreadLocal` without `remove()`** in pools (user context, trace baggage)—mitigate with **try/finally remove** or avoid ThreadLocal on pooled threads. **Static collections** holding request-scoped objects—mitigate with **instance-scoped** structures or weak references only when semantics allow. **Class loader leaks** (hot redeploy, OSGi-style plugins)—mitigate with **separate class loaders per deploy**, restart policy, or isolate plugins. **Effective leak:** **open HTTP/DB connections** or **native buffers** not released—shows as RSS growth with flat heap; use **try-with-resources**, pool limits, and leak detection on pools.

---

## 9. A Go service’s goroutine count grows unbounded after a deploy—how is this a memory problem and how do you fix it?

**Answer:** Each goroutine has a **small stack that grows on demand** (typically KB→MB under deep calls); **millions of blocked goroutines** mean **GB of stack + scheduler metadata** and **GC scanning more roots**—p99 rises even if heap objects look small. Common causes: **blocked sends/receives** on unbuffered channels, **missing `ctx.Done()`** on outbound HTTP, **worker loops without exit**, **errgroup** without cancel on first error. Fix: **propagate `context.Context`**, bounded worker pools, **timeouts** on I/O, channel buffer + consumer health checks. Diagnose: `pprof/goroutine?debug=2`, search for repeated stacks stuck on `chan receive` or `select`. Prevention in design review: **max concurrency per dependency**, document who **closes** channels ([Ch. 05 Go depth](../05-java-and-golang-deep-dive/README.md)).

---

## 10. Design memory limits for a Java order API in Kubernetes: 4 GiB pod, 2k RPS, JSON payloads ~50 KB. What do you document?

**Answer:** **Estimate:** allocation per request × RPS → young GC frequency; 50 KB JSON might allocate **several×** parsed size in objects—rough **100–300 MB/s** allocation rate is plausible before optimization, so **p99 GC** matters more than average heap. **Heap:** start **`-Xmx2g -Xms2g`** (or 2.5g) leaving **≥1–1.5 GiB** for metaspace, threads (~200–400 threads × 1 MB stacks if not virtual), direct buffers (size Netty pools explicitly). **Container:** `resources.limits.memory: 4Gi`; **do not** set `-Xmx` to 4g. **JDK 21+:** consider **virtual threads** to cut stack RAM vs platform threads; watch **pinning** blocking carrier threads. **Observability:** JFR or Micrometer `jvm.gc.pause`, allocation alerts, **OOMKilled** dashboard, heap dump runbook on canary. **Scale lever:** reduce allocations (streaming JSON, object pools where profiled), then horizontal pod autoscaling on **CPU + p99**, not heap % alone. Cross-link: [Ch. 27 Performance](../27-performance-engineering/README.md), [Ch. 06 Concurrency](../06-concurrency-and-multithreading/README.md).
