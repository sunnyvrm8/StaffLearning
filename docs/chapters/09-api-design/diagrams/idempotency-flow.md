# Idempotent Write — Request Lifecycle

**Supports decision:** Where to store idempotency records, how long to retain them, and what to return on duplicate keys.

```mermaid
sequenceDiagram
  participant C as Client
  participant A as API
  participant S as Idempotency store
  participant D as Downstream

  C->>A: POST /charges Idempotency-Key: k1
  A->>S: lookup k1
  alt first request
    S-->>A: miss
    A->>D: charge
    D-->>A: 201 + body
    A->>S: save k1 + response hash
    A-->>C: 201 Created
  else duplicate in flight
    S-->>A: locked
    A-->>C: 409 Conflict retry later
  else duplicate completed
    S-->>A: cached response
    A-->>C: replay same status + body
  else prior failed terminal
    S-->>A: failed marker
    A-->>C: 422 or same error as first attempt
```

**Caption:** The store is the source of truth for “have we already executed this intent?”—not the client’s retry counter. TTL must exceed client retry windows and reconciliation jobs.
