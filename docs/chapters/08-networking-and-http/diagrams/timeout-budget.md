# Networking and HTTP — Timeout Budget

**Supports decision:** How to split connect vs read vs user-facing deadline so retries do not exceed SLA.

```mermaid
flowchart LR
  subgraph user [User SLA 500ms]
    U[Browser or BFF deadline]
  end

  subgraph edge [Gateway 450ms]
    G[Ingress timeout]
  end

  subgraph svc [Service 400ms]
    S[Handler context]
  end

  subgraph deps [Dependencies parallel]
    D1[Fraud 200ms connect plus read]
    D2[Inventory 200ms connect plus read]
  end

  U --> G --> S
  S --> D1
  S --> D2
  S --> M[Margin 50ms encode and GC]
```

**Caption:** Each inner box must finish before its parent; retries and hedging consume the same budget unless explicitly excluded in the design doc.
