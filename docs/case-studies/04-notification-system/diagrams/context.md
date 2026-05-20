# Notification System — Context

**Supports decision:** show the flow from event ingestion to provider delivery and DLQ.

```mermaid
flowchart TB
  source[Event Source]
  api[Ingestion API]
  prefs[Preference Service]
  queue[(Channel Queue)]
  worker[Delivery Worker]
  provider[Provider API]
  dlq[DLQ]

  source --> api --> prefs
  prefs --> queue
  queue --> worker --> provider
  worker --> dlq
```