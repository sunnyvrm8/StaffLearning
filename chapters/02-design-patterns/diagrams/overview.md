# Design Patterns — Forces to Pattern Families

**Supports decision:** Name the right pattern family in a review (behavioral vs structural) before debating class diagrams.

```mermaid
flowchart TB
  subgraph forces [Common production forces]
    f1[Varying algorithm per tenant or region]
    f2[Legacy or vendor API shape mismatch]
    f3[Fan-out on domain event]
    f4[Cross-cutting metrics auth cache]
    f5[Downstream failure cascades]
  end

  subgraph behavioral [Behavioral - often first choice]
    strategy[Strategy]
    observer[Observer / domain events]
    template[Template Method]
  end

  subgraph structural [Structural - boundaries]
    adapter[Adapter]
    facade[Facade]
    decorator[Decorator]
  end

  subgraph enterprise [Enterprise - resilience]
    breaker[Circuit Breaker]
    bulkhead[Bulkhead]
  end

  f1 --> strategy
  f2 --> adapter
  f3 --> observer
  f4 --> decorator
  f5 --> breaker
  f5 --> bulkhead
```

**Reading the diagram:** Start from the **force** (what hurts in prod), not the pattern name. If the force is “varying policy,” Strategy beats Factory unless you are constructing incompatible families of objects.
