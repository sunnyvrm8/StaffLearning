# Microservices — Gateway, BFF, and Mesh

**Supports decision:** where to terminate TLS, auth, and routing (gateway) vs shape responses per client (BFF) vs enforce east-west policy (mesh) without duplicating all three on day one.

```mermaid
flowchart LR
  subgraph northSouth [North-South]
    client[Client]
    gw[API Gateway]
    bff[BFF]
    svc[Core API]
    client --> gw
    client --> bff
    gw --> svc
    bff --> svc
  end

  subgraph eastWest [East-West]
    a[Service A]
    m[Mesh sidecar]
    b[Service B]
    a --> m --> b
  end

  subgraph meshDuties [Mesh when justified]
    mtls[mTLS]
    retry[Retry budgets]
    trace[Trace propagation]
  end

  m -.-> mtls
  m -.-> retry
  m -.-> trace
```
