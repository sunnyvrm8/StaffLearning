# SQL vs NoSQL Selection

**Supports decision:** Picking a primary store when requirements mix transactions, flexible schema, and access patterns.

```mermaid
flowchart TD
  start[New datastore choice]
  start --> q1{Need multi-row ACID<br/>on business invariants?}
  q1 -->|yes| sql[Relational OLTP<br/>Postgres / MySQL]
  q1 -->|no| q2{Dominant access pattern?}
  q2 -->|key-value / session| kv[Redis / Dynamo-style KV]
  q2 -->|wide-column time series| wc[Cassandra / Bigtable-style]
  q2 -->|document flexible schema| doc[MongoDB / Document DB]
  q2 -->|graph traversals| graph[Graph DB]
  sql --> q3{Global scale + partition?}
  q3 -->|single region first| sql
  q3 -->|later| shard[Shard by tenant / user_id]
  doc --> q4{Cross-document transactions required?}
  q4 -->|yes| sql
  q4 -->|no| doc
```
