# Kafka and Messaging — Architecture Overview

**Supports decision:** Choose log-based Kafka when multiple independent consumers need durable replay vs a delete-on-ack work queue.

```mermaid
flowchart LR
  subgraph producers [Producers]
    checkout[Checkout Service]
    catalog[Catalog Service]
  end

  subgraph cluster [Kafka Cluster]
    broker1[(Broker)]
    broker2[(Broker)]
    topicOrder[[order-events topic]]
    topicNotify[[notification-commands topic]]
    broker1 --- topicOrder
    broker2 --- topicOrder
    broker1 --- topicNotify
  end

  subgraph consumers [Consumer Groups]
    shipGroup[Fulfillment CG]
    analyticsGroup[Analytics CG]
    notifyGroup[Notifier CG]
  end

  checkout -->|produce keyed by orderId| topicOrder
  catalog -->|produce| topicNotify
  topicOrder --> shipGroup
  topicOrder --> analyticsGroup
  topicNotify --> notifyGroup
```

**Caption:** One topic fan-out to **multiple consumer groups**—each group tracks its own offsets. Fulfillment lag does not block analytics if each group scales independently (subject to partition count).
