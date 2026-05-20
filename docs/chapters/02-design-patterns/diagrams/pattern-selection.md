# Pattern Selection — Introduce Indirection?

**Supports decision:** Push back on pattern fever or justify a port/strategy/breaker in the next sprint.

```mermaid
flowchart TD
  start[New integration or behavior variant] --> variant{Second real variant on roadmap?}
  variant -->|no| yagni[Ship simplest module - YAGNI]
  variant -->|yes| boundary{Crosses team or vendor boundary?}
  boundary -->|yes| port[Port + Adapter]
  boundary -->|no| algo{Algorithm family varies?}
  algo -->|yes| strat[Strategy registry]
  algo -->|no| review[Revisit change vector - maybe SRP split only]
  port --> unstable{Dependency outages hurt core path?}
  unstable -->|yes + fallback| breaker[Add circuit breaker + timeout]
  unstable -->|yes no fallback| escalate[Product + SRE sign-off on fail-fast]
  unstable -->|no| done[Port alone may suffice]
```

**Caption:** “Second real variant” means committed roadmap or second tenant/region—not architect imagination.
