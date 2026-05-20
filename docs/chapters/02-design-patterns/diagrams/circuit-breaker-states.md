# Circuit Breaker State Machine

**Supports decision:** Configure thresholds, half-open probes, and fallback behavior when a fraud or enrichment dependency degrades.

```mermaid
stateDiagram-v2
  [*] --> Closed
  Closed --> Open: failureRate >= threshold
  Open --> HalfOpen: waitDuration elapsed
  HalfOpen --> Closed: probeCalls succeed
  HalfOpen --> Open: probeCalls fail
  note right of Open
    Fail fast
    optional fallback
    alert on open
  end note
```

**Align with timeouts:** Client deadline should be **less than** user-facing SLA; breaker open should return before pool exhaustion—not after 30s of blocked threads.
