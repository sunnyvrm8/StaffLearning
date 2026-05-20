# Error and Cancellation Propagation

**Supports decision:** whether to use exceptions across a module boundary, how to map errors at gRPC/HTTP layers, and where deadlines must be enforced.

```mermaid
sequenceDiagram
  participant C as Client
  participant A as API handler
  participant B as Downstream

  Note over C,B: Java — timeout on future / gRPC deadline
  C->>A: request
  A->>B: call with deadline
  alt success
    B-->>A: result
    A-->>C: 200
  else timeout or cancel
    B-->>A: TimeoutException / Status.DEADLINE_EXCEEDED
    A-->>C: 504 / mapped error body
  end

  Note over C,B: Go — context.WithTimeout
  C->>A: request with ctx
  A->>B: Do(ctx, req)
  alt ctx done
    B-->>A: context.DeadlineExceeded
    A-->>C: 504 + err wrap chain
  end
```

**Caption:** Java often centralizes failure as thrown types; Go returns `error` and uses `context` for cancellation. **Map at the edge**—do not leak internal exception names or `sql.ErrNoRows` verbatim to clients.
