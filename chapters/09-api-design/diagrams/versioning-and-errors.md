# Versioning and Error Propagation

**Supports decision:** Pick URL/header versioning for public REST and map internal failures to stable, machine-readable client errors.

```mermaid
flowchart LR
  subgraph public [Public REST v2]
    req[Request Accept: application/vnd.co.v2+json]
    handler[v2 handlers]
  end

  subgraph internal [Internal gRPC]
    rpc[PaymentService.Charge]
  end

  subgraph map [Error mapping layer]
    mapErr[Map codes to Problem+JSON]
  end

  req --> handler
  handler --> rpc
  rpc -->|status + details| mapErr
  mapErr -->|RFC 7807 type + retryable| req
```

```mermaid
stateDiagram-v2
  [*] --> v1_active
  v1_active --> dual_run: ship v2 additive
  dual_run --> v1_deprecated: sunset date set
  v1_deprecated --> v1_removed: metrics below threshold
  v1_removed --> [*]
```

**Caption:** Version states are operational commitments—monitor traffic share per version before removal. Never leak stack traces or vendor codes across the public boundary.
