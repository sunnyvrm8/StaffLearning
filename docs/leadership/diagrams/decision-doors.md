# Decision Making — One-Way vs Two-Way Doors

**Supports decision:** How much process, review, and rollback design a decision deserves before commit.

```mermaid
flowchart TB
  subgraph input [Incoming decision]
    Q{Reversible within<br/>one sprint at<br/>acceptable cost?}
  end

  subgraph twoWay [Two-way door]
    DRI[Name single DRI]
    FAST[Time-box options<br/>e.g. 48-72h]
    SHIP[Ship behind flag<br/>or additive change]
    LEARN[Measure and revert<br/>if wrong]
  end

  subgraph oneWay [One-way door]
    RFC[Written RFC:<br/>options + trade-offs]
    REVIEW[Cross-team review<br/>security SRE legal as needed]
    ROLLBACK[Explicit rollback or<br/>forward-fix plan]
    ADR[Record ADR and owner]
  end

  Q -->|Yes| twoWay
  Q -->|No or uncertain| oneWay
```

**Caption:** Two-way doors optimize for **learning speed**; one-way doors optimize for **wrong-direction cost** (data loss, public API breakage, vendor lock-in, regulatory exposure).

**Examples**

| Two-way | One-way |
|---------|---------|
| Feature flag for new checkout step | Choosing primary payment PSP for 3 years |
| Add optional JSON field to event | Delete column with no backfill plan |
| Pilot team uses new cache library | Commit to multi-region active-active for orders |
