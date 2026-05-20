# Chapter 08: Networking and HTTP

> **One line:** Every outbound API call is a chain of DNS, TCP, TLS, and HTTP semantics—misconfigured timeouts or pools turn “the dependency is slow” into your service’s p99 cliff.

## Why this matters in production

A payments gateway reports healthy upstream latency in dashboards, yet checkout p99 jumps from 120 ms to 2 s during a marketing push. Traces show threads blocked in `socketRead0`, connection pool wait queues spiking, and occasional **30 s** stalls matching the default read timeout nobody documented. Stakeholders feel **failed authorizations** and **support volume**; the root cause is often **network stack policy** (DNS TTL, TLS session reuse, pool size, timeout budget), not application logic.

This chapter is the substrate for [Chapter 09: API Design](../09-api-design/README.md) (contracts and errors), [Chapter 15: Load Balancing](../15-load-balancing-and-traffic-management/README.md) (L7 routing), [Chapter 17: Microservices](../17-microservices-architecture/README.md) (gateways, mesh), and [Chapter 20: Distributed Systems Fundamentals](../20-distributed-systems-fundamentals/README.md) (retries, partitions, backpressure). You cannot design reliable sync calls without naming what happens below `GET /orders/{id}`.

## Core ideas

### DNS: resolution, caching, and failure modes

**Intuition:** DNS is a distributed cache with TTLs—your client (or libc, or the JVM, or CoreDNS) remembers answers until they expire.

| Layer | What caches | Ops signal |
|-------|-------------|------------|
| **Browser / app** | Per-process resolver cache | Stale IP after blue/green cutover |
| **OS resolver** | `nscd`, systemd-resolved | Pod still hits drained node |
| **Kubernetes** | CoreDNS + `ndots`, search paths | `svc.cluster.local` vs FQDN latency |
| **CDN / GeoDNS** | Edge routing | Region mis-route after failover |

**Production patterns:** Prefer **stable names** (service discovery, load balancer DNS) over hard-coded IPs. Set **reasonable connect timeouts** shorter than read timeouts so a bad DNS answer fails fast. For multi-region failover, coordinate **TTL** with traffic shift (low TTL = faster failover, more query load). Watch **DNS latency** in traces (`dns.lookup` span) when p99 grows without CPU spike.

### TLS: handshake cost and session reuse

**Intuition:** TLS adds one or two round trips before HTTP bytes flow; reusing sessions amortizes that cost across many requests on the same connection.

| Mechanism | Benefit | Caveat |
|-----------|---------|--------|
| **TLS 1.3** | Fewer RTTs vs 1.2 | Cipher suite and cert chain still matter |
| **Session resumption** (ticket / ID) | Skip full handshake on new TCP | Tickets must rotate; sticky to wrong LB breaks |
| **mTLS (mesh)** | Identity at transport | Cert rotation, SPIFFE/SPIRE ops burden |

**Architect note:** Cold starts (scale-out, new pods) pay **handshake storms** if every request opens a new connection—pair TLS tuning with **connection pooling** and **warmup** on deploy. Terminate TLS at the **gateway** vs **app** is a security/compliance trade-off (PCI, HSM, WAF inspection)—not “always pass through.”

### HTTP/1.1, HTTP/2, and HTTP/3

| Version | Multiplexing | Head-of-line | Typical use |
|---------|--------------|--------------|-------------|
| **HTTP/1.1** | One request per connection (pipelining rare) | Per-connection HOL | Legacy, simple proxies |
| **HTTP/2** | Many streams per TCP connection | **TCP-level** HOL if one stream stalls | gRPC, browser APIs, service mesh |
| **HTTP/3** | QUIC over UDP, per-stream flow control | Less TCP HOL; UDP path issues | Mobile, lossy networks, some CDNs |

**HTTP/2 in microservices:** One connection to an upstream can carry dozens of RPCs—great for efficiency, dangerous if a **slow stream** blocks the TCP window (monitor **SETTINGS**, stream resets, `GOAWAY`). **gRPC** is HTTP/2—client and server **max concurrent streams** and **keepalive** must match ([Chapter 09](../09-api-design/README.md)).

**HTTP/3:** Useful when last-mile loss is high; ops cost is **UDP reachability** (firewalls, middleboxes) and dual-stack fallbacks. Do not enable by default without measuring your client fleet.

### Timeouts: budget the whole chain

**Intuition:** A timeout is a promise to stop waiting and fail—if every layer uses the full budget, users wait forever.

Layered budget example for a **500 ms** checkout SLA calling fraud + inventory:

```text
Client total:     500 ms  (user-facing)
  Gateway:        450 ms  (propagate deadline)
    Fraud call:   200 ms  (connect 100 ms + read 150 ms)
    Inventory:    200 ms  (parallel with fraud)
  Margin:          50 ms  (serialization, GC, retry NOT included)
```

| Timeout type | What it bounds | Typical mistake |
|--------------|----------------|-------------------|
| **Connect** | TCP + TLS handshake | Same as read → hangs on black-hole IP |
| **Read / response** | Time between bytes or full body | Too high → thread exhaustion |
| **Write** | Upload stall | Large payload to webhooks |
| **Idle (pool)** | Reclaim stale sockets | Firewall dropped NAT without FIN |

**Go:** `context.WithTimeout` on the request; tune `http.Transport` dial and `ResponseHeaderTimeout`. **Java:** `HttpClient` connect timeout + `orTimeout` on `CompletableFuture` or request timeout APIs; align with **virtual threads** so blocking I/O scales but **pool limits** still cap fan-out.

**Rule:** Child deadline **≤ parent deadline − margin**. Retries consume the same budget ([Chapter 20](../20-distributed-systems-fundamentals/README.md)).

### Connection pools and keep-alive

**Intuition:** Opening a new TCP+TLS connection per request burns RTTs and FDs; pools reuse warm connections—until they go stale or you exhaust the pool.

| Knob | Too low | Too high |
|------|---------|----------|
| **Max connections per route** | Queueing under burst (pool wait in traces) | Memory, upstream overload |
| **Max idle / TTL** | Frequent handshakes | Stale connection to drained backend |
| **Keep-alive** | Latency tax | LB thinks client still on dead pod |

**Symptoms:** Rising **pool pending** metric, `ConnectionPoolTimeoutException`, goroutines blocked in `semacquire` on transport. **Fix order:** verify upstream health and LB drain → right-size pool → reduce per-request connection churn (batch, HTTP/2) → shorten idle timeout if middleboxes reset silently.

**Service mesh:** Sidecar adds another hop and pool; “double pooling” (app + Envoy) can hide saturation—align **max connections** with HPA and upstream limits.

## When to use / when to avoid

**Use explicit network policy when:**

- Building sync clients to payment, fraud, or identity providers with hard SLAs.
- Operating behind NAT, corporate proxies, or strict firewalls (HTTP/3, WebSockets).
- Scaling RPS where handshake cost dominates traces.
- Running in Kubernetes with cluster DNS and rolling deploys.

**Avoid over-tuning when:**

- Traffic is batch/offline (throughput matters more than connection reuse).
- A managed SDK hides transport (still document timeouts at the SDK boundary).
- You have not measured—default 30 s read timeouts mask design bugs.

## How it fails

| Symptom | Likely cause | What to check |
|---------|--------------|---------------|
| p99 stairs every N seconds | DNS TTL refresh, GC less often | DNS span duration, cache hit |
| Spike after deploy | Cold TLS, empty pools, new pods | Handshake rate, `connect` span |
| Steady 30 s errors | Default read timeout, hung upstream | Timeout config diff per env |
| 502 after scale-in | Stale keep-alive to drained IP | LB health check grace, idle timeout |
| Retry storm | No jitter, timeout > gateway budget | Retry count metrics, 429/503 ratio |
| “Works in dev” | `localhost` skips DNS/TLS path | Same hostname + TLS as prod |

**Debugging hooks:** Distributed tracing (`net.peer.name`, `http.url` redacted), client-side metrics (connect time, TTFB, pool active/idle), synthetic checks from **same network path** as prod (not laptop-only).

## Architect takeaway

- **Decide:** Timeout budget table per dependency; TLS termination point; HTTP/2 vs 1.1 per upstream; pool max per route vs fleet QPS.
- **Measure:** Connect vs TTFB vs total; pool wait time; DNS lookup p95; TLS handshake rate per instance.
- **Document in design review:** Deadline propagation (`traceparent` / `grpc-timeout`), retry policy vs idempotency, behavior when pool is exhausted (fail fast vs queue), and blast radius of a slow dependency under HTTP/2 multiplexing.

## Diagrams

- [Request path overview](./diagrams/overview.md) — DNS → TCP → TLS → HTTP
- [Timeout budget](./diagrams/timeout-budget.md) — layered deadlines
- [Connection pool lifecycle](./diagrams/connection-pool.md) — reuse, idle eviction, saturation

## Code examples

| Scenario | Java | Go |
|----------|------|-----|
| Outbound payment client: connect/read timeouts + bounded pool | [java/HttpsClientTimeoutsPool.java](./java/HttpsClientTimeoutsPool.java) | [go/http_client_pool_timeouts.go](./go/http_client_pool_timeouts.go) |

**Production note:** Ship one **HttpClient** / `http.Client` per upstream dependency (or per trust boundary), configured in code or config service—not per request `new` client. Wire pool and timeout metrics before tuning max connections.

## Related topics

- [Chapter 09: API Design](../09-api-design/README.md) — idempotency, error mapping, gRPC deadlines
- [Chapter 15: Load Balancing and Traffic Management](../15-load-balancing-and-traffic-management/README.md) — health checks, connection draining
- [Chapter 20: Distributed Systems Fundamentals](../20-distributed-systems-fundamentals/README.md) — retries, circuit breakers, backpressure
- [Chapter 26: Observability](../26-observability/README.md) — RED metrics, trace propagation
- [Chapter 06: Concurrency](../06-concurrency-and-multithreading/README.md) — thread/goroutine blocking on I/O

## Interview preparation

See [interview-questions.md](./interview-questions.md) (**Top 10** with answers — DNS/TLS, HTTP versions, timeouts, pools, and gateway-scale debugging).
