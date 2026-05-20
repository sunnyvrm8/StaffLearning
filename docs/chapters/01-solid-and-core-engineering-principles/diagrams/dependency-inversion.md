# Dependency Inversion — Payment Port

**Supports decision:** Where to place interfaces so domain logic stays testable and PSP migration does not rewrite `OrderService`.

```mermaid
flowchart LR
  subgraph domain [Domain / Application]
    orderSvc[OrderService]
    port[PaymentPort]
    orderSvc --> port
  end

  subgraph infra [Infrastructure]
    stripe[StripeAdapter]
    adyen[AdyenAdapter]
    fake[InMemoryFake]
  end

  port -.->|implements| stripe
  port -.->|implements| adyen
  port -.->|implements| fake

  subgraph external [External]
    stripeApi[(Stripe API)]
    adyenApi[(Adyen API)]
  end

  stripe --> stripeApi
  adyen --> adyenApi
```

**CI hook:** Dependency rule — `domain` must not import `infra` or vendor SDK packages; only `infra` references adapters.
