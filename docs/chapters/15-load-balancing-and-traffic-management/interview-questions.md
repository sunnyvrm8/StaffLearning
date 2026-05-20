# Interview Questions: Load Balancing and Traffic Management

**Bank size:** 10  
**Rationale:** Traffic-path chapter bridging single service to fleet; rubric 10 for L4/L7 and health-check drill.  
**Last updated:** 2026-05-20

---

## Core

## 1. What is the difference between L4 and L7 load balancing?

**Answer:** **L4 (TCP/UDP):** routes by IP/port—fast, protocol-agnostic, no cookie/path awareness; used for **databases, gRPC passthrough, TLS passthrough**. **L7 (HTTP):** understands host, path, headers—**path-based routing**, TLS termination, WAF, sticky cookies, rate limits. Pain: putting TLS termination at L7 adds CPU but enables **routing and auth** at the edge. Trade-off: L7 inspects content (latency ~0.1–2 ms); L4 simpler under extreme connection churn.

---

## 2. Compare round-robin, least connections, and weighted routing for a mixed fleet.

**Answer:** **Round-robin:** equal requests per backend—fails when request costs vary (one heavy report API instance overloaded). **Least connections:** better for **long-lived** or variable-duration work (WebSocket, slow clients). **Weighted:** send more to bigger instances or canary 5% to v2. Production: use **least connections** or **power of two choices** for HTTP with keep-alive; combine with **active health checks** so dead nodes aren’t in rotation.

---

## 3. Why are health checks critical, and what is the difference between shallow and deep checks?

**Answer:** LB removes unhealthy targets—without checks, traffic hits **dying pods** (502 storm). **Shallow:** TCP open or `GET /health` static—fast, may lie (JVM up, DB down). **Deep:** validates **dependencies** with timeout—safer for readiness, risky for liveness if deps flap. Pattern: **readiness** deep (remove from LB), **liveness** shallow. Interval ~5–10 s; **unhealthy threshold** avoids flapping. Connection draining on shutdown prevents in-flight drop.

---

## 4. What are sticky sessions (session affinity), and when should you avoid them?

**Answer:** Same client → same backend via cookie or **consistent hash** on client IP. Use when **server-local session state** exists and migration is hard. Avoid for **elastic scale** and failover—lost node loses sessions; prefer **centralized session store** (Redis) or JWT. If required, set **short TTL** and fallback to shared store. CDN/API: stateless is default for 10x scale ([Chapter 28](../28-scalability-and-capacity-planning/README.md)).

---

## 5. Explain connection draining (deregistration delay) during deploys.

**Answer:** On scale-in or deploy, LB stops **new** connections to a target but allows **in-flight** requests to finish (30–300 s depending on p99 request duration). Without draining, long uploads or checkout calls **abort** mid-flight. K8s `preStop` + `sleep` + readiness false mirrors this. Measure **5xx spike** at deploy boundary—if spike, increase drain or fix app graceful shutdown.

---

## 6. An API gateway sits in front of 40 microservices. What belongs at the gateway vs in each service?

**Answer:** **Gateway:** TLS termination, authn (JWT validate), **rate limiting**, routing, WAF, request size limits, **correlation IDs**, optional aggregation. **Service:** authz (ownership), business validation, data access. Anti-pattern: **all logic in gateway**—becomes monolith. Trade-off: central policy vs **per-team autonomy**—use mesh (mTLS, retries) for east-west ([Chapter 17](../17-microservices-architecture/interview-questions.md)).

---

## Stretch

## 7. Users in EU must hit EU backends for compliance. How do you route at the edge?

**Answer:** **GeoDNS** or **Anycast** to regional L7 LB; route by **latency** or explicit tenant region header; avoid cross-border data paths in application logs/traces. Failover: EU region down → **documented** break-glass (fail to US read-only?) vs hard fail. Test **DNS TTL** (60–300 s) for failover speed. Numbers: cross-region RTT 80–150 ms—routing wrong region is a compliance and latency incident.

---

## 8. p99 latency spikes only on one AZ. The load balancer looks healthy. What next?

**Answer:** Check **per-AZ target health**, **partial backend pool**, **subnet routing**, or **noisy neighbor** on one node group. Compare **LB access logs** per AZ; **synthetic probes** from outside. Possible **asymmetric routing** (request enters AZ-a, DB in AZ-b). Fix: **AZ-aware routing**, rebalance targets, or drain bad AZ. Cross-link observability traces by `availability_zone` tag ([Chapter 26](../26-observability/README.md)).

---

## 9. Design traffic flow for canary release of `checkout-service` at 5% with automatic rollback.

**Answer:** **L7** weighted route or service mesh **VirtualService** 95/5; monitor **golden signals** (5xx rate, p99, business success rate) with **5–15 min** window and error budget. Auto-rollback when 5xx > 2× baseline or payment failure metric trips. Require **backward-compatible** API and schema. **Hash-based** canary (sticky user cohort) vs random 5%—product may prefer cohort consistency. Link CI/CD ([Chapter 13](../13-docker-and-cicd/interview-questions.md)).

---

## 10. When do you terminate TLS at the load balancer vs at the pod?

**Answer:** **At LB:** central cert management, hardware/software offload, simpler pod config—LB sees plaintext (secure VPC). **At pod (mTLS):** **zero-trust** east-west, end-to-end encryption, compliance—more CPU and cert rotation per service. Hybrid: TLS to ingress, **mTLS** inside mesh. Decision: threat model (insider VPC sniffing) vs operational cost; regulated data often wants **TLS end-to-end** minimum to ingress + mTLS internal.
