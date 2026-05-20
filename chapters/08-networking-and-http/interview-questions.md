# Interview Questions: Networking and HTTP

**Top 10** with answers — DNS, TLS, HTTP/2–3, timeouts, connection pools, and gateway-scale failure modes.  
**Last updated:** 2026-05-20

---

## Core

## 1. Walk through what happens when your order service calls `https://payments.example.com/charge` for the first time on a fresh pod.

**Answer:** The JVM or Go runtime asks the **resolver** (libc, CoreDNS, or corporate DNS)—often **1–50 ms** unless cache is cold. **TCP connect** to the resolved IP (or load balancer VIP) runs under your **connect timeout**; failure here is “connection refused” or timeout, not HTTP 502. **TLS handshake** adds **1–2 RTTs** (TLS 1.3 fewer than 1.2); no session ticket yet. **HTTP request** goes out—likely **HTTP/2** if ALPN negotiates it—then you wait for **TTFB** bounded by **read/response timeout**. First call pays full cost; subsequent calls on the **same pooled connection** skip TCP+TLS if keep-alive and **session resumption** hold. In Kubernetes, also account for **sidecar** hop (mesh): DNS and connect may target `127.0.0.1` with different pool behavior. Document each phase in traces so incidents do not blame “payments is slow” without separating **DNS vs connect vs TLS vs upstream processing**.

---

## 2. Why can low DNS TTL help failover but hurt steady-state latency—and what would you set for a multi-region payment API?

**Answer:** **Low TTL** (e.g., 30–60 s) lets clients pick up **new IPs quickly** after region failover or blue/green, reducing traffic to drained nodes. Cost: more **DNS queries**, resolver load, and occasional **latency spikes** on TTL expiry when every pod refreshes at once. **High TTL** (300–3600 s) is stable and fast but **extends blast radius** if you forget to lower TTL before a planned cutover. For **payments**, use a **stable CNAME** to the provider’s anycast or regional endpoint; coordinate with their **runbook** (they often control authoritative TTL). Client-side: **cache respect** + **connect timeout** shorter than read timeout so bad records fail fast. Measure **DNS lookup duration** in traces; alert if p95 jumps after deploys. Do not hard-code IPs unless you accept manual failover.

---

## 3. Compare HTTP/1.1, HTTP/2, and HTTP/3 for service-to-service calls behind a mesh.

**Answer:** **HTTP/1.1:** one in-flight request per connection unless you open **many parallel connections**—simple but **FD and handshake heavy** at high QPS. **HTTP/2:** **multiplexes** streams on one TCP connection—efficient for **gRPC** and chatty APIs; risk is **TCP head-of-line blocking** if one stream or loss stalls the connection—watch slow streams and **GOAWAY** on deploy. **HTTP/3 (QUIC):** per-stream loss recovery, better on **lossy mobile** paths; ops friction includes **UDP blocked**, dual-stack fallback, and less uniform middlebox support. **Mesh:** sidecars speak HTTP/2 between proxies; app may still use HTTP/1.1 to localhost sidecar. **Pick H2** for internal RPC by default; **H3** when measured client benefit exceeds ops cost—not because it is newer.

---

## 4. How do you layer connect, read, and end-to-end timeouts so a 500 ms checkout SLA is not silently violated?

**Answer:** Start from the **user-facing budget** (500 ms) and subtract **gateway, serialization, and margin** (~50–100 ms) → **~400 ms** left for dependency work. Set **connect timeout** (100–200 ms) **shorter than read** so black-hole routes fail before burning the whole budget. **Read/response timeout** caps **TTFB + body** per call; for parallel fraud + inventory at **200 ms each**, the parent context must be **≤400 ms** with **shared deadline** (`context`, `grpc-timeout`, `traceparent` deadline). **Never** stack retries that each use full 500 ms—**retry only with idempotency** and **remaining budget** ([Ch. 09](../09-api-design/README.md), [Ch. 20](../20-distributed-systems-fundamentals/README.md)). Document a **table per dependency** in the design review; defaults (30 s) are an anti-pattern.

---

## 5. What is connection pooling solving, and what symptoms indicate the pool—not the upstream—is the bottleneck?

**Answer:** Pooling **reuses TCP+TLS** sessions, cutting **RTT and CPU** per request and capping **file descriptors** versus unbounded `connect()` per call. **Healthy pool:** stable **active/idle** counts, connect span rare after warmup. **Pool as bottleneck:** rising **wait time** before send, `ConnectionPoolTimeoutException` (Java), goroutines blocked on `http.Transport` semaphores (Go), **queue depth** metrics while upstream latency looks fine. Causes: **max connections per route too low** for burst QPS, **too many client instances** each with its own pool, **HTTP/1.1** without enough parallel connections, or **slow streams on H2** blocking reuse. Fix: **one shared client per upstream**, right-size **MaxConnsPerHost** against upstream guidance and **HPA max pods × conns**, align **idle timeout** with LB **drain** interval—not only “increase pool” without limit.

---

## Stretch

## 6. After a blue/green deploy, error rate spikes with connection resets—no application stack trace. What network-layer hypotheses do you test first?

**Answer:** **Stale keep-alive:** clients still send on connections to **drained pods**; LB returns RST. Mitigate: lower **idle timeout** on client below LB drain, use **graceful GOAWAY** on H2, verify **readiness** removes endpoints before SIGTERM. **DNS/cache:** resolvers still return **old IPs** if TTL high—check `dig` from **inside the cluster**. **TLS mismatch:** new cert chain, wrong SNI, or **mTLS** rotation on mesh. **Security group / NACL** change blocking new subnets. **Test:** curl from a **canary pod** with same ServiceAccount and mesh config; compare **connect** vs **TLS** vs **HTTP** span; tcpdump only on canary if needed. Roll forward fix: **connection max age** (Envoy `max_connection_duration`) forces periodic refresh.

---

## 7. Explain TLS session resumption and when it fails after scale-out.

**Answer:** **Session tickets / IDs** let client and server skip full handshake on a **new TCP** connection if both support the same ticket key material. **Fails or degrades** when: **new pods** do not share ticket keys (no distributed ticket encryption), **LB without sticky** sends resumption to a server that does not recognize the ticket, **key rotation** without dual-key period, or **TLS termination** moves from app to gateway (different session cache). Symptom: **handshake rate** spikes correlate with **new replicas** or deploys, p99 up while business logic flat. Mitigate: terminate at layer with **shared session cache**, use **TLS 1.3** (faster full handshake anyway), and rely on **HTTP/2 connection reuse** so resumption matters less. Security: ticket keys must **rotate**—document rotation runbook.

---

## 8. A gRPC service shows healthy CPU but clients time out—how does HTTP/2 behavior explain this?

**Answer:** gRPC runs on **HTTP/2 streams** sharing one TCP connection. A **slow or hung RPC** can consume **flow-control window**, delaying other RPCs on the same connection—**logical head-of-line** even without CPU load. **Max concurrent streams** mis-tuned, missing **keepalive** (middlebox closes idle TCP while app thinks connection is open), or **server not sending WINDOW_UPDATE** produce client timeouts with **idle server CPU**. **GOAWAY** during rolling deploy without client retry stranding calls. Debug: **grpc-channel** metrics, per-method latency, **reset/stream error** counts; enable **keepalive** within provider limits; use **separate channels** to isolate **bulk** from **latency-sensitive** methods if needed. Contrast with **HTTP/1.1** where slow call blocks one connection but others may exist on separate sockets.

---

## 9. Design outbound networking policy for a BFF calling five internal REST services under a 300 ms p99 SLA at ~2k RPS.

**Answer:** **Per dependency:** connect **100 ms**, read **150–200 ms** (tighter for critical path), **shared HttpClient** with **max connections** ≈ `(2k RPS × latency) / instances` with headroom—e.g., 50–100 per route per pod after division. **Parallelize** independent calls with **parent context 280 ms**; sequential chain must fit in one budget or **degrade** (cached catalog, default fraud score). **HTTP/2** to mesh ingress; enforce **deadline propagation** headers. **Retries:** only on **idempotent GET** or keyed writes, max 1, **jitter**, budget-aware. **Observability:** connect/TTFB histograms, pool wait, DNS p95. **Scale 10×:** avoid linear connection growth—**batch APIs**, caching ([Ch. 12](../12-caching-strategies/README.md)), async for non-critical paths. Document **failure mode when pool exhausted:** fail fast 503 vs unbounded queue (queue risks thread/goroutine pile-up, [Ch. 06](../06-concurrency-and-multithreading/README.md)).

---

## 10. Weak vs strong answer: “We’ll use the default HTTP client and fix latency later.”

**Answer:** **Weak:** Accepts framework defaults (often **30 s read**, unbounded or per-request clients), no **deadline propagation**, no metrics on **connect vs upstream**, and discovers pool exhaustion in production under marketing load. **Strong:** Publishes a **timeout budget matrix**, one **configured client per upstream** with **connect/read/idle** tuned to SLA, **HTTP/2** where appropriate, **idempotent retry policy** tied to remaining budget, dashboards for **DNS/TLS/connect/TTFB/pool wait**, and load tests that include **cold start + deploy** (handshake storm). States trade-off: aggressive timeouts increase **503** unless paired with **fallbacks and circuit breakers**—prefer explicit failure over hung threads. This is the bar for [Ch. 09 API Design](../09-api-design/README.md) and gateway reviews.
