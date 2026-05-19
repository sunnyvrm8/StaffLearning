# SOLID — Change Pressure on Checkout

**Supports decision:** Which principle applies when a new fee rule, PSP, or read model lands—without redrawing the whole system.

```mermaid
flowchart TB
  subgraph checkout [Checkout Monolith - logical view]
    orderSvc[OrderService]
    feeEngine[FeeEngine]
    payOrch[PaymentOrchestrator]
    ledger[LedgerWriter]
    notify[NotificationEmitter]
  end

  subgraph changeVectors [Common change vectors]
    cv1[New card network / PSP]
    cv2[New fee or tax rule]
    cv3[New refund policy]
    cv4[New analytics sink]
  end

  cv1 -->|DIP port + adapter| payOrch
  cv2 -->|OCP strategy registry| feeEngine
  cv3 -->|LSP-safe contracts| payOrch
  cv4 -->|SRP split module| notify

  orderSvc --> feeEngine
  orderSvc --> payOrch
  payOrch --> ledger
  orderSvc --> notify
```

**Reading the diagram:** Each arrow is a *type* of future change. Map your roadmap item to a letter (SRP/O/L/I/D) before choosing patterns from Chapter 02.
