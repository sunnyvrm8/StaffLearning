# API Design — REST vs gRPC Overview

**Supports decision:** Choose public HTTP/JSON vs internal binary RPC based on consumer, evolution speed, and operability—not team preference alone.

```mermaid
flowchart TB
  subgraph clients [Consumers]
    web[Web / mobile]
    partner[Partner integrations]
    svc[Internal services]
  end

  subgraph edge [Edge]
    gw[API Gateway / BFF]
  end

  subgraph apis [Contract surfaces]
    rest[REST JSON OpenAPI]
    grpc[gRPC protobuf]
  end

  subgraph core [Domain services]
    orders[Order service]
    pay[Payment service]
  end

  web --> gw
  partner --> gw
  gw --> rest
  svc --> grpc
  rest --> orders
  grpc --> orders
  grpc --> pay
  orders --> pay
```

**Caption:** Partners and browsers hit versioned REST at the gateway; service-to-service calls use gRPC where schema discipline and streaming matter. Both paths should share domain nouns and error semantics even when wire formats differ.
