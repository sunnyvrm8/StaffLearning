# Java vs Go — Runtime and Concurrency Model

**Supports decision:** sizing thread pools / `GOMAXPROCS`, choosing virtual threads vs platform threads, and setting per-instance concurrency caps.

```mermaid
flowchart TB
  subgraph jvm [Java JVM]
    appJ[Application code]
    vt[Virtual threads / platform threads]
    jmm[Heap + metaspace]
    gcJ[G1 / ZGC]
    appJ --> vt
    appJ --> jmm
    jmm --> gcJ
    vt --> osJ[OS threads - carrier pool]
  end

  subgraph goruntime [Go runtime]
    appG[Application code]
    g[g Goroutines]
    sched[Scheduler]
    heapG[Heap]
    gcG[Go GC]
    appG --> g
    g --> sched
    appG --> heapG
    heapG --> gcG
    sched --> osG[OS threads GOMAXPROCS]
  end
```

**Caption:** Java stacks are often larger (platform threads) or mounted on carriers (virtual threads); Go goroutines start small and grow. Both multiplex onto OS threads—**blocking syscalls and locks** still matter for tail latency.
