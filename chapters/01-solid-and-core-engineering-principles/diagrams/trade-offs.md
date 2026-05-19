# When to Introduce Abstraction

**Supports decision:** OCP/DIP now vs YAGNI—avoid speculative layers on the critical path.

```mermaid
flowchart TD
  start[New requirement arrives]
  freq{Second variant likely in 2 quarters?}
  blast{Change touches stable hot path?}
  team{Multiple teams same module?}
  yagni[Keep simple - inline or config]
  strategy[OCP - registry / strategy]
  port[DIP - port + adapter]
  split[SRP - new module or package]

  start --> freq
  freq -->|no| yagni
  freq -->|yes| blast
  blast -->|yes| strategy
  blast -->|no| team
  team -->|yes| split
  team -->|no| port
```

**Caption:** “Second variant” means a committed roadmap item (second PSP, second fee jurisdiction)—not architect intuition alone.
