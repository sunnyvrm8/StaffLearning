# Data Structures and Complexity — Mental Model

**Supports decision:** Whether a slowdown is algorithmic (wrong structure) or environmental (I/O, locks, GC)—before rewriting working code.

```mermaid
flowchart TB
  subgraph inputs [Inputs]
    n[Problem size n]
    ops[Operation mix read/write/scan]
    sla[Latency SLA]
  end

  subgraph analysis [Complexity lens]
    n --> bigO[Asymptotic class O / Theta / Omega]
    ops --> amortized[Amortized vs worst case]
    sla --> constant[Hidden constants and cache locality]
  end

  subgraph pick [Structure choice]
    bigO --> array[Array / slice contiguous]
    bigO --> hash[Hash map expected O1]
    bigO --> tree[Tree / heap ordered ops]
    bigO --> graph[Graph BFS DFS shortest path]
  end

  subgraph validate [Production validate]
    pick --> profile[Profile hot path]
    profile --> measure[p99 latency and allocations]
  end
```
