# Ports and Adapters (Hexagonal)

**Supports decision:** Where to draw the boundary so domain logic never imports vendor SDKs—and how strangler migrations swap implementations.

```mermaid
flowchart TB
  subgraph domain [Domain - policy]
    orderSvc[OrderService]
    port[InventoryPort]
    orderSvc --> port
  end

  subgraph adapters [Adapters - infrastructure]
    legacy[LegacyWarehouseAdapter]
    modern[ModernWMSAdapter]
    fake[InMemoryFakeAdapter]
  end

  subgraph external [External systems]
    soap[(Legacy SOAP API)]
    rest[(REST WMS)]
  end

  port --> legacy
  port --> modern
  port -.->|tests| fake
  legacy --> soap
  modern --> rest
```

**Migration note:** Route traffic by feature flag or tenant from `LegacyWarehouseAdapter` to `ModernWMSAdapter` while `OrderService` stays unchanged—strangler at the port, not rewrite of checkout.
