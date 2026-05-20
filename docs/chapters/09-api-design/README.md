# Chapter 09: API Design (REST, gRPC, Versioning, Idempotency, Pagination, Errors)

> **One line:** An API is a promise about behavior under retries, time, and multiple teams—design the contract before you split the monolith.

## Why this matters in production

A marketplace splits checkout into six microservices without agreeing on **idempotency**, **error shapes**, or **pagination**. Mobile clients retry `POST /orders` on flaky networks; two charges land. Partner integrations parse `error: something went wrong` strings that change every deploy. Support cannot correlate tickets because `request_id` appears in three headers. Stakeholders feel **double billing**, **integration breakage**, and **slow partner onboarding**—not “we should have used protobuf.”

API design is **contract design**: nouns and verbs that match domain language ([Chapter 03: DDD](../03-domain-driven-design-and-bounded-contexts/README.md)), transport choices that match consumers ([Chapter 08: Networking](../08-networking-and-http/README.md)), and failure semantics that survive partial outages ([Chapter 20: Distributed Systems](../20-distributed-systems-fundamentals/README.md), [Chapter 23: Idempotency & Sagas](../23-idempotency-sagas-and-distributed-transactions/README.md)). Nail this chapter **before** service boundaries harden; retrofitting idempotency and versioning across twenty teams is a multi-quarter tax.

## Core ideas

### REST vs gRPC: who is the consumer?

| Dimension | REST (HTTP/JSON) | gRPC (HTTP/2, protobuf) |
|-----------|------------------|-------------------------|
| **Best for** | Browsers, partners, public docs, caching | Service-to-service, streaming, strict schemas |
| **Contract** | OpenAPI + conventions; flexible, drift-prone | `.proto` + codegen; breaking change is explicit |
| **Debugging** | curl, browser devtools, ubiquitous proxies | grpcurl, mesh tooling; binary needs tooling |
| **Evolution** | Additive JSON fields; version URLs/headers | Field numbers, `optional`, package versioning |
| **Risk** | Ambiguous status codes, fat DTOs | Tight coupling if protos leak to public edge |

**Production anchor:** Public checkout stays **REST** behind a gateway/BFF; internal `OrderService → PaymentService` uses **gRPC** with shared deadline propagation. Both should map to the same domain events and error *meaning* even when wire format differs.

**Avoid:** gRPC through the public internet without a gateway translation layer unless every client is under your control. **Avoid:** REST between fifty Java services with hand-written DTOs and no schema registry when you already run a mesh—pick one internal standard.

See [diagrams/overview.md](./diagrams/overview.md).

### Resource modeling and HTTP semantics

**Intuition:** URLs name **resources**; methods express **safe vs unsafe** intent.

| Method | Safe | Idempotent | Typical use |
|--------|------|------------|-------------|
| GET | Yes | Yes | Read, cacheable |
| PUT | No | Yes | Replace (whole resource) |
| PATCH | No | No* | Partial update (*often made idempotent with ETags) |
| POST | No | No | Create, commands, non-idempotent actions |
| DELETE | No | Yes | Remove |

**Commands vs resources:** `POST /orders` (create) is fine; `POST /orders/{id}/cancel` is a **command** on a resource—document whether cancel is idempotent (usually yes with idempotency key or natural key). Payments often expose `POST /charges` with a client key rather than overloading PUT.

**Stakeholder pain:** Teams use `GET` with body or `POST` for reads to “avoid caching”—breaks intermediaries and CDNs. Architects standardize: reads are GET; sensitive reads use POST with **no** side effects and document as non-cacheable.

### Versioning: never surprise a partner

| Strategy | Mechanism | When |
|----------|-----------|------|
| **URL** | `/v2/orders` | Obvious, easy routing at gateway; noisy URLs |
| **Header** | `Accept: application/vnd.company.orders+json;version=2` | Cleaner URLs; harder to test in browser |
| **Query** | `?api-version=2024-01-01` | Stripe-style date versions; good for SaaS |
| **Protobuf** | New package `v2`, new RPC, dual deploy | Internal services |

**Rules that survive audits:**

1. **Additive first** — new optional fields, new endpoints; never rename or repurpose fields in place.
2. **Sunset with metrics** — traffic to v1 &lt; 1% for 30 days before removal.
3. **Document breaking changes** in changelog + migration guide, not only Slack.

See [diagrams/versioning-and-errors.md](./diagrams/versioning-and-errors.md).

### Idempotency keys: safe retries

**Intuition:** The client sends a **unique key per intent**; the server executes at most once and returns the **same outcome** for duplicates.

- Header: `Idempotency-Key: <uuid>` (Stripe-style) on `POST` writes.
- Store: Redis (fast, TTL) or DB unique constraint (durable); retain **24–72 h** or until reconciliation completes.
- Responses: replay **same status + body**; distinguish **in-flight** (409) vs **completed** (replay 201).

**How it fails:** Key scoped to wrong tenant; store TTL shorter than client retries → duplicate charge. Key reused for **different** payloads → wrong replay. No lock → double execution under concurrency.

See [diagrams/idempotency-flow.md](./diagrams/idempotency-flow.md) and code below.

### Pagination: OFFSET vs cursor (keyset)

| Style | Mechanism | Scales? | Pitfall |
|-------|-----------|---------|---------|
| **Offset** | `?page=3&size=50` | Poor on large tables (scans, drift) | Rows shift between pages |
| **Cursor (keyset)** | `?cursor=opaque&limit=50` | Yes with index on sort keys | Sort change breaks cursor |
| **Time-based** | `?since=2024-01-01T00:00:00Z` | Event feeds, audit | Clock skew, duplicates |

**Production:** Public list APIs for orders, messages, ledger entries use **keyset** on `(created_at, id)` with opaque cursor. Admin UIs with &lt;10k rows may use offset.

### Errors: stable, machine-readable, actionable

**Intuition:** Clients branch on **type**, humans read **detail**, operators trace **instance/correlation id**.

- **RFC 7807 Problem Details** (`application/problem+json`): `type` (URI), `title`, `status`, `detail`, `instance`.
- Extend with **`retryable`**, `error_code` (stable enum), `request_id`—never stack traces on public APIs.
- Map internal gRPC `status` + `details` to public problems at the **BFF/gateway**—do not leak vendor codes.

| HTTP status | Meaning for clients |
|-------------|---------------------|
| 400 | Fix request (validation) |
| 401 / 403 | AuthN vs AuthZ |
| 404 | Resource missing (or hidden) |
| 409 | Conflict, duplicate, in-flight idempotency |
| 422 | Semantically invalid (business rule) |
| 429 | Rate limited — honor `Retry-After` |
| 503 | Transient — retry with backoff |

**gRPC mapping:** `INVALID_ARGUMENT` → 400, `NOT_FOUND` → 404, `ALREADY_EXISTS` → 409, `UNAVAILABLE` → 503 with retry hint.

### Contract-first before microservices

**Intuition:** Bounded context boundaries should appear as **API modules** in a monolith or **OpenAPI/proto repos** before separate deployables.

1. **Nouns align with aggregates** — `Order`, `Shipment`, not `OrderManagerService` RPC soup.
2. **Publish OpenAPI / protos** as versioned artifacts in CI; breaking diff fails build.
3. **Consumer-driven contract tests** — checkout team tests against payment stub generated from published spec.
4. **Error catalog** shared across services — one page of `type` URIs and retry policy.

Splitting without this yields **distributed monolith**: synchronous chains, incompatible errors, and “we’ll fix versioning in v2.”

## When to use / when to avoid

**Use when:**

- Multiple clients (web, partners, mobile) need predictable evolution.
- Writes are retried (mobile, message consumers, load balancer retries).
- You are drawing a service boundary—freeze the contract first.

**Avoid when:**

- Internal-only script called once—plain function call, no HTTP theater.
- Premature **GraphQL** on a team without resolver performance discipline (N+1, authz per field).
- **Version explosion**—more than two supported public versions without automated deprecation.

## How it fails

| Symptom | Likely cause | What to check |
|---------|--------------|---------------|
| Duplicate charges | Missing idempotency or TTL too short | Idempotency hit rate, key collision logs |
| Partner outage on deploy | Removed field, changed error string | Traffic per API version, schema diff in CI |
| Slow list API | OFFSET on 50M rows | Query plans, cursor adoption |
| Mystery 500s | Unmapped exceptions | Problem `type` coverage, gRPC→HTTP mapper tests |
| Retry storm | 503 without jitter | Client retry metrics, `Retry-After` compliance |

**Debugging hooks:** Per-route metrics (latency, 4xx/5xx by `error_code`), trace `request_id` from gateway through gRPC metadata, audit log of idempotency key outcomes.

## Architect takeaway

- **Decide:** Public REST vs internal gRPC; versioning strategy; idempotency store and TTL; pagination style per resource; error schema (Problem Details + extensions).
- **Measure:** Version traffic share; idempotency replay rate; p99 list latency; 4xx ratio by `type`; client retry counts after 503.
- **Document in design review:** Idempotency scope (per user? per merchant?); breaking change policy; max page size; rate limits; correlation ID header name; sunset dates for old versions.

## Diagrams

- [REST vs gRPC overview](./diagrams/overview.md)
- [Idempotency request lifecycle](./diagrams/idempotency-flow.md)
- [Versioning and error propagation](./diagrams/versioning-and-errors.md)

## Code examples

| Scenario | Java | Go |
|----------|------|-----|
| Idempotency key on charge | [java/IdempotencyKeyHandler.java](./java/IdempotencyKeyHandler.java) | [go/idempotency_key.go](./go/idempotency_key.go) |
| Cursor (keyset) pagination | [java/CursorPagination.java](./java/CursorPagination.java) | [go/cursor_pagination.go](./go/cursor_pagination.go) |
| RFC 7807 Problem Details | [java/ProblemDetailsError.java](./java/ProblemDetailsError.java) | [go/problem_details.go](./go/problem_details.go) |

**Production note:** Ship idempotency and structured errors on the **first** payment or order write API—retrofit is harder than partition keys. Wire correlation IDs at the gateway and pass through gRPC metadata (`x-request-id` → `grpc-metadata-x-request-id`).

## Related topics

- [Chapter 08: Networking and HTTP](../08-networking-and-http/README.md) — timeouts, pools, HTTP/2 underpin gRPC and REST performance.
- [Chapter 03: Domain-Driven Design](../03-domain-driven-design-and-bounded-contexts/README.md) — bounded contexts become API boundaries.
- [Chapter 17: Microservices](../17-microservices-architecture/README.md) — gateways, BFFs, and sync chains after contracts exist.
- [Chapter 23: Idempotency, Sagas](../23-idempotency-sagas-and-distributed-transactions/README.md) — distributed write patterns beyond single-request keys.
- [Chapter 25: Security](../25-security-architecture/README.md) — OAuth scopes, mTLS, and field-level authz on APIs.

## Interview preparation

See [interview-questions.md](./interview-questions.md) (50 questions — core handbook chapter per rubric).
