# Interview Questions: API Design

**Bank size:** 50  
**Rationale:** Core handbook chapter (multi-concept: REST, gRPC, versioning, idempotency, pagination, errors) per interview-bank-rubric.  
**Last updated:** 2026-05-20

---

## Foundations

## 1. What is the difference between an API and an implementation, and why does that distinction matter before a microservices split?

**Answer:** The **API** is the published contract—resources, operations, schemas, error semantics, SLAs—that consumers depend on. The **implementation** is how you fulfill it today (monolith module, stored procedure, queue worker). Teams that split services without freezing the contract ship **accidental interfaces** tied to internal tables; every refactor becomes a breaking change for partners. Treat OpenAPI/proto artifacts as versioned products with compatibility tests; implementation can move behind the contract as long as behavior and observability match.

---

## 2. Define safe, idempotent, and idempotent-enough in HTTP. Which methods are which?

**Answer:** **Safe** methods do not change server state (GET, HEAD, OPTIONS)—caches and crawlers may invoke them. **Idempotent** methods produce the same *effect* on the resource when repeated (PUT, DELETE; GET is both safe and idempotent). **POST** is neither safe nor idempotent by default—payment and order creation need **idempotency keys** or natural keys to become safe under retries. **PATCH** is often non-idempotent unless you use ETags/If-Match or design replace semantics. “Idempotent-enough” in production means retries from mobile, gateways, or message consumers do not double-charge or duplicate rows.

---

## 3. When is REST the right default for a new surface, and when should you reach for gRPC first?

**Answer:** **REST/JSON** when consumers are browsers, partners, third-party integrators, or you need human-debuggable traffic, CDN caching, and widespread tooling—typically the **public edge** and BFF. **gRPC** when both ends are your services, you want strict protobuf schemas, low overhead, bi-di streaming, and codegen in Java/Go—typically **east-west** calls. Hybrid is normal: REST at the gateway, gRPC inside. Wrong default: gRPC to partners who cannot maintain protobuf pipelines; REST between 40 internal services with no schema discipline.

---

## 4. What belongs in a resource URL vs the request body vs headers?

**Answer:** **URLs** identify resources (`/orders/{id}`) and should be stable, bookmarkable, loggable—avoid secrets in paths. **Body** carries representations and command payloads (create order, patch fields). **Headers** carry cross-cutting metadata: auth, `Accept`/`Content-Type`, `Idempotency-Key`, `If-Match`, trace IDs, optional API version. Anti-pattern: all actions as `POST /doThing` with opaque bodies—hard to document and cache. Another anti-pattern: tenant id only in body when every route should scope by `X-Tenant-Id` or path prefix for authz middleware.

---

## 5. Explain RFC 7807 Problem Details in one paragraph. What fields must clients rely on?

**Answer:** Problem Details (`application/problem+json`) standardize errors with **`type`** (URI identifying the problem class), **`title`** (short human summary), **`status`** (HTTP code), **`detail`** (occurrence-specific explanation), and **`instance`** (URI for this occurrence, often the request path). Clients should branch automation on **`type`** or a stable `error_code` extension—not on `title`/`detail` text that copywriters change. Operators use `instance` plus correlation IDs in logs. Never expose stack traces or SQL in public fields.

---

## 6. What is the purpose of an idempotency key header on `POST /charges`?

**Answer:** It names a **client intent** so duplicate deliveries (retry, timeout uncertainty, at-least-once messaging) execute the charge **at most once** and return the **same response** for the same key and payload scope. The server stores key → outcome for a TTL exceeding retry windows. Without it, HTTP POST retries are undefined—double capture is a common payments incident. Keys must be unique per logical operation, not reused across different amounts or merchants.

---

## 7. Compare offset pagination and cursor (keyset) pagination for a 50M-row `orders` table.

**Answer:** **Offset** (`LIMIT 50 OFFSET 10000`) forces the database to scan and discard rows—cost grows with page depth; concurrent inserts cause **skipped or duplicate** rows across pages. **Keyset** (`WHERE (created_at, id) < ($cursor) ORDER BY created_at DESC LIMIT 50`) uses an index seek—stable cost per page and consistent under append-heavy workloads. Trade-off: cursors bind to a **sort order**; changing sort invalidates cursors. Public APIs at scale default to opaque cursors; internal admin tools on small datasets may use offset.

---

## 8. What is the difference between 401 Unauthorized and 403 Forbidden in API design?

**Answer:** **401** means authentication failed or is missing—client should obtain/refresh credentials (`WWW-Authenticate`). **403** means the caller is authenticated but not allowed for this resource/action—retrying the same token will not help. Mixing them breaks client libraries and security audits. For public APIs, avoid using 404 to hide existence of resources the caller cannot read—document policy (404 vs 403) consistently for IDOR prevention vs debuggability.

---

## 9. What does “contract-first” mean in practice before extracting a payment service from a monolith?

**Answer:** Publish **OpenAPI or proto** for `Charge`, `Refund`, errors, and idempotency rules; generate server stubs and **consumer contract tests**; run breaking-change detection in CI. Domain code depends on generated interfaces, not ad-hoc JSON maps. Monolith modules call the same interface in-process; later, an HTTP/gRPC adapter implements it remotely. Payment and checkout teams agree on nouns and failure codes **before** separate deployables and on-call rotations.

---

## 10. How do HTTP status codes relate to gRPC status codes at a gateway?

**Answer:** Map at the **edge** so clients see one model: e.g. `INVALID_ARGUMENT` → 400, `NOT_FOUND` → 404, `ALREADY_EXISTS` → 409, `FAILED_PRECONDITION` → 412/422, `UNAVAILABLE` → 503 with retry guidance, `DEADLINE_EXCEEDED` → 504. Preserve **retryable** as an extension on Problem Details. Do not pass raw gRPC messages to partners—translate to stable `type` URIs and log internal cause separately.

---

## Application

## 11. Design the headers and response shape for `POST /v2/orders` that creates an order and may be retried by mobile clients.

**Answer:** Require `Idempotency-Key: <uuid v4>` scoped per user/session; `Content-Type: application/json`; `Authorization: Bearer …`; optional `X-Request-Id` (or generate server-side). Body: line items, shipping, payment method reference—no full PAN. Success: **201** with `Location: /v2/orders/{id}`, body with order id and status. Duplicate key with same body: replay **201** and same body. In-flight duplicate: **409** with `retryable: true`. Validation: **400** Problem with field errors extension. Idempotency record TTL ≥ 48h; store hash of canonical body to reject key reuse with different payload (**422**).

---

## 12. How would you version a public REST API when adding a required field to `CreateOrder`?

**Answer:** Prefer **additive** evolution: new optional field with server default, or new endpoint `POST /v3/orders` if the field is truly required and semantics change. If required field is unavoidable, ship **v3** with sunset timeline for v2; never silently reject v2 clients. Document migration; monitor traffic share per version. For protobuf internal APIs, add field with new number; never reuse numbers. Breaking without version bump is how partner integrations break on deploy Thursday.

---

## 13. Sketch pagination query parameters and response fields for `GET /messages` on a chat product with real-time inserts.

**Answer:** `GET /messages?channel_id=c1&limit=50&cursor=<opaque>`. Response: `{ "items": [...], "next_cursor": "...", "has_more": true }`. Cursor encodes keyset on `(sequence, id)` descending. `limit` max 100 enforced server-side. No `page=`. Document that cursors expire after 24h or on sort change. For “latest first” UI, first page omits cursor; subsequent pages pass `next_cursor`. Rate-limit per channel to prevent scrape.

---

## 14. A partner asks for synchronous `POST /transfer` but your ledger is async. What HTTP semantics do you use?

**Answer:** Return **202 Accepted** with `Location: /transfers/{id}` and body `{ "id", "status": "pending" }` when work is queued; **200/201** only when committed synchronously if SLA allows (&lt;300ms). Provide `GET /transfers/{id}` for status polling or webhooks for completion. Document that retries on 202 must use idempotency key. Problem if partner treats 202 as failure—onboard with examples. Align with [Chapter 18: Event-Driven](../18-event-driven-architecture/README.md) outbox patterns for final state.

---

## 15. How do you document rate limits in an HTTP API?

**Answer:** Return **429** with `Retry-After` (seconds or HTTP-date), Problem `type` for rate limit, and optional headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset` per window (user, IP, or API key). Document limits per tier in OpenAPI description and developer portal. Clients implement exponential backoff on 429/503. Ops: alert on 429 spike (attack vs misconfigured client). Do not rate-limit auth failures the same as business endpoints without tuning.

---

## 16. What fields should a public error response include for a declined card on checkout?

**Answer:** Problem Details: `type` URI e.g. `.../card-declined`, `title`, `status` **402** or **422** per your policy (be consistent), `detail` safe for display (“Card declined”), `instance` `/orders/{id}`, extensions: `error_code: CARD_DECLINED`, `retryable: false`, `request_id` for support. Do **not** return issuer raw codes, AVS full strings, or PCI data. Log rich internal context server-side mapped to `request_id`.

---

## 17. When should you use PUT vs PATCH vs POST for updating order shipping address?

**Answer:** **PATCH** partial update `{ "shipping": { ... } }` with optional `If-Match: etag` for optimistic concurrency—fits most e-commerce edits. **PUT** replace entire order representation—rare, risky if clients send partial state. **POST** `/orders/{id}/shipping-address` as a command—good when the action has side effects (re-rate shipping, re-validate inventory). Pick one style per API surface and document; mixing all three confuses generated clients.

---

## 18. How do you expose bulk export of 10M records to an enterprise partner without melting the API?

**Answer:** Avoid giant synchronous `GET`. Offer **async job**: `POST /exports` → **202** + `export_id`, `GET /exports/{id}` for status, signed URL to object storage when ready. Alternatively **cursor-paginated** `GET` with strict rate limits and `limit` cap. Stream CSV with chunked transfer if synchronous window is required and SLA allows hours. Never OFFSET deep pages. Contract retention and PII scope in the job definition.

---

## Design & Trade-offs

## 19. REST with OpenAPI vs gRPC with protobuf for internal service communication—argue both sides for a 30-service estate.

**Answer:** **REST/OpenAPI:** universal debugging, easier hiring/partner parity, works through corporate proxies, JSON logs readable. Cost: schema drift, larger payloads, weaker streaming story. **gRPC:** codegen, binary efficiency, streaming, clear breaking-change rules via protos. Cost: tooling investment, load balancer/mesh config, harder ad-hoc curls. At 30 services, architects often standardize **gRPC inside** with a **proto repo** and REST only at the edge—unless org lacks proto maturity, then strict OpenAPI + JSON schema CI is the alternative. Either way, one source of truth and breaking-change gates—not both ad hoc.

---

## 20. URL path versioning (`/v1/`) vs `Accept` header versioning—which do you pick for a B2B SaaS API?

**Answer:** **URL versioning** is obvious in docs, logs, routing rules, and partner support—“call `/v2/`”. Cost: ugly URLs, proliferation of route trees. **Accept/header** keeps URLs clean, suits hypermedia purists; harder for customers to test in browser without examples. **Date-based query** (`api-version=2025-03-01`) works well for Stripe-like SaaS with frequent additive releases. Pick one primary strategy; avoid three concurrent mechanisms. B2B SaaS often chooses **URL or date version** for support clarity.

---

## 21. GraphQL at the public API gateway—when is it worth it vs REST?

**Answer:** **Worth** when many clients need different field subsets, you have strong resolver performance discipline (DataLoader, batching), centralized authz per field, and mobile wants one round trip. **Avoid** as default when team lacks N+1 controls, caching at CDN is important, partners expect simple REST, or rate limiting per-query cost is immature. Many architects use **GraphQL at BFF** for owned apps and **REST for partners**. Payments and webhooks rarely start GraphQL-first.

---

## 22. Should idempotency keys be scoped per user, per merchant, or globally unique?

**Answer:** Scope keys **per authorization principal** (merchant API key or user id) plus store namespace in the idempotency table—prevents collision when two tenants accidentally use the same UUID. Optionally require merchant id in key material. Global uniqueness without tenant scope is an IDOR risk if keys are guessable. Document that keys are opaque UUIDs, not sequential. Retention and uniqueness constraint: `UNIQUE(merchant_id, idempotency_key)`.

---

## 23. Compare embedding errors in 200 responses vs using proper 4xx/5xx status codes.

**Answer:** **200 with `{ "success": false }` }** breaks HTTP semantics—caches, monitors, and middleware mis-classify failures; clients need custom parsers everywhere. **Proper status codes** integrate with retries (503), auth (401/403), and Problem Details. Exception: legacy systems you wrap at gateway—translate to correct HTTP at the edge. Internal gRPC may use `OK` with business status in body only if gateway maps to HTTP errors for public callers.

---

## 24. Webhooks vs polling for notifying partners of order shipment—API design trade-offs.

**Answer:** **Webhooks:** push latency low; require signature (HMAC), replay protection, idempotent handler, DLQ, and partner retry documentation. **Polling:** simpler for partners, higher load on your `GET /orders/{id}`; use ETag/`updated_since` cursor to reduce payload. Hybrid: webhooks primary, polling fallback. Contract: delivery guarantees are **at-least-once**—partners must dedupe by event id. Align with [Chapter 19: Kafka](../19-kafka-and-messaging/README.md) for internal fan-out before edge webhook dispatchers.

---

## 25. Fat DTO vs multiple small endpoints for order checkout—how do you decide?

**Answer:** **Fat DTO** (`GET /checkout-context`) reduces round trips on high-latency mobile—good when cached and versioned carefully. **Small resources** (`/cart`, `/shipping-options`, `/tax`) improve cache granularity and team ownership but cause chatty networks and distributed transaction risk. BFF aggregates for your app; public partners get stable coarse resources with field masks or `include=` sparse fieldsets. Measure p99 with real packet loss; 8 serial REST calls often lose to one composed read or HTTP/2 multiplexing.

---

## 26. Proto backward compatibility: what changes are safe without a new package version?

**Answer:** Safe: add **optional** fields with new field numbers; add new RPCs; add enum values if clients ignore unknown (document). Unsafe: renumber fields, change wire type, rename fields without reserved numbers, delete required fields, change RPC semantics on same name. Use `reserved` for retired fields. Run **buf breaking** or similar in CI. For public REST JSON, analogous rule: add optional keys, never rename or change type in place.

---

## 27. API gateway terminates auth vs service validates JWT—split of responsibility?

**Answer:** **Gateway:** TLS termination, rate limit, API key → identity, optional JWT validation for coarse routing, request size limits, WAF. **Service:** fine-grained authz (owns this order?), business validation, idempotency, data scope. Duplicating all auth logic only in gateway couples security to one team; validating only in each service duplicates bugs. Pattern: gateway validates token signature/issuer/expiry; services trust signed internal identity headers **only on mTLS mesh**, never blindly from internet. Document threat model for header spoofing.

---

## 28. You inherit APIs with no correlation id. What standard do you introduce?

**Answer:** Accept `X-Request-Id` from clients or generate UUID at edge; propagate through access logs, MDC, gRPC metadata (`x-request-id`), and Problem `instance`/`request_id` extension. Return it on every response header. Document in OpenAPI. Backfill dashboards to group by request id. Within a week of deploy, support can trace checkout failures across payment and inventory. Pair with OpenTelemetry trace id when tracing matures.

---

## Coding

## 29. Implement idempotent handling for `POST /charges`: pseudocode for lookup, lock, execute, store.

**Answer:** On request: validate `Idempotency-Key` present; canonicalize body hash; `GET store(key)` → if completed, return cached status/body; if in-flight lock exists, return **409**; acquire lock (SET NX TTL); execute charge; on success `SET store(key) = {status, body, hash}` release lock; on failure store terminal error if non-retryable. Lock TTL prevents stuck locks. Scope key by merchant. See [IdempotencyKeyHandler.java](./java/IdempotencyKeyHandler.java) and [idempotency_key.go](./go/idempotency_key.go).

---

## 30. Write the SQL shape for keyset pagination on `orders(created_at, id)` descending, page size 50.

**Answer:** Given cursor decoded to `(cursor_created_at, cursor_id)`: `SELECT id, created_at, ... FROM orders WHERE (created_at, id) < ($1, $2) ORDER BY created_at DESC, id DESC LIMIT 51` — fetch 51 to detect `has_more`, return 50, encode last row as next cursor. First page: omit WHERE or use sentinel max timestamp. Index: composite `(created_at DESC, id DESC)`. Never `OFFSET` for page 10,000.

---

## 31. Map gRPC `Status.NOT_FOUND` and `Status.ALREADY_EXISTS` to HTTP for a BFF.

**Answer:** `NOT_FOUND` → **404** Problem `type` resource-not-found. `ALREADY_EXISTS` → **409** conflict (duplicate create) with `retryable: false` unless in-flight idempotency. Implement a single `GrpcStatusMapper.toProblem(Status, metadata)` used by all routes—unit test matrix for all codes you emit. Log gRPC `details` server-side only.

---

## 32. Encode and decode an opaque cursor from `(order_id, created_at)` without leaking sort internals.

**Answer:** Serialize `order_id|created_at`, Base64url encode (no padding), return as `next_cursor`. Decode server-side only; reject malformed cursors with **400**. Sign cursor (HMAC) if clients must not tamper with sort keys. Version cursor prefix byte if schema changes (`v1.`). See [CursorPagination.java](./java/CursorPagination.java).

---

## 33. Define a Java or Go record/struct for RFC 7807 Problem Details plus `retryable` and `error_code`.

**Answer:** Fields: `type` (URI string), `title`, `status` (int), `detail`, `instance` (optional URI), `retryable` (bool), `error_code` (enum string). Serialize as `application/problem+json`. Factory methods per domain error (`insufficientFunds(orderId)`). See [ProblemDetailsError.java](./java/ProblemDetailsError.java) and [problem_details.go](./go/problem_details.go).

---

## 34. Client retries `POST` three times after 503—what server-side guards are required besides idempotency keys?

**Answer:** Idempotency store; distinguish **retryable** 503 vs terminal 4xx; `Retry-After` header; downstream timeouts shorter than client deadline; circuit breaker to fail fast; dedupe at provider if external API. Without keys, POST retries are unsafe. Log retry count via `X-Retry-Count` if client sends it. Ensure load balancer does not retry non-idempotent POST bodies to different app versions with incompatible behavior.

---

## 35. OpenAPI: how do you represent optional fields and breaking vs non-breaking changes in CI?

**Answer:** Mark optional fields without `required` array entry; use `nullable` deliberately. CI: oasdiff or openapi-diff fails on removed properties, type changes, new required fields, enum removals. Allow additive optional properties and new endpoints. Pin spec version to git tag; consumers generate clients in pipeline. Breaking change requires major version bump per policy.

---

## 36. gRPC: set deadline from incoming HTTP request with 5s budget left—what propagates?

**Answer:** Compute `remaining = client_deadline - now - margin`; create gRPC `Context` with `deadline` or `CallOptions.withDeadlineAfter(remaining)`. Propagate via interceptors on client stub. If remaining ≤ 0, fail fast **504** without calling downstream. Child calls get min(parent, local budget). Never use unbounded blocking calls on gateway threads.

---

## 37. Validate idempotency key reuse: same key, different JSON body—what HTTP status and why?

**Answer:** Return **422 Unprocessable Entity** (or **400**) with Problem `type` idempotency-key-mismatch—client bug reusing key for new intent. Do not execute second operation or overwrite silently—that caused reconciliation nightmares in payments. Log security alert if pattern looks malicious. First request’s body hash stored with key.

---

## 38. Implement ETag optimistic concurrency for `PATCH /orders/{id}` in outline.

**Answer:** `GET` returns `ETag: "v3"` (version hash). `PATCH` requires `If-Match: "v3"`; on version mismatch **412 Precondition Failed** with current ETag so client refreshes. On success, increment version, return new ETag. Prevents lost updates when two clients patch shipping. Alternative: `version` integer in body with same semantics.

---

## System Design

## 39. Design the public API layer for a marketplace: buyers (web/mobile), sellers (dashboard), partners (ERP). REST, gRPC, gateways?

**Answer:** **Buyers:** REST/JSON via CDN-friendly BFF; OAuth; cursor lists; idempotent checkout POST. **Sellers:** REST with richer admin resources or separate BFF; higher rate limits. **Partners:** versioned REST (`/v2`), API keys, webhooks for fulfillment, async exports. **Internal:** gRPC between catalog, inventory, payments with shared proto repo. **API gateway:** auth, rate limit, WAF, request id, TLS. Estimate: buyer peak ~5–20k RPS read-heavy, writes ~500 RPS with strict idempotency on checkout. Document error catalog and sunset policy per audience.

---

## 40. A monolith will split into Order, Payment, Inventory. What contracts do you publish in month one?

**Answer:** OpenAPI/proto for: `CreateOrder`, `AuthorizePayment`, `ReserveInventory`, cancel/refund flows; idempotency on all writes; Problem error catalog; pagination on list endpoints; correlation id header. Consumer-driven tests from Order team against Payment mock. Non-goals: do not expose internal DB ids across contexts—use public ids. Event schemas for async completion if sync chain is temporary. See [Chapter 03: DDD](../03-domain-driven-design-and-bounded-contexts/README.md) for boundary alignment.

---

## 41. Design rate limiting for a public API: 1000 req/min per API key, burst allowed. Components?

**Answer:** Token bucket or leaky bucket at gateway (Redis or envoy rate limit service); key = API key id; return 429 + `Retry-After`. Separate buckets for expensive endpoints (`POST /search`). Store config per tier in control plane. Metrics: allowed vs rejected; alert on single key consuming 50% capacity (bug or abuse). Document headers. Consider fairness vs noisy neighbor on shared Redis.

---

## 42. Mobile app offline queue replays 50 writes—how does the API stay safe?

**Answer:** Every mutating call carries **idempotency key** generated once per user action at queue time; server dedupes. Timestamp ordering client-side does not guarantee server order—use server-assigned sequence on sync. Return stable errors for conflicts (**409**) so app can surface merge UI. Expose `GET /sync` with cursor since `last_sync_token`. TTL idempotency store &gt; max offline duration (e.g. 7 days). Test replay storm in staging.

---

## 43. Multi-region active-active API—what changes for idempotency and pagination?

**Answer:** Idempotency store must be **replicated** or region-sticky routing (same key → same region) to avoid double execution. Cursors may be region-local unless global DB—document stickiness. Conflict resolution for writes: leader per entity or CRDT last-writer-wins—API must expose version/ETag. Public DNS/geo routes users; internal replication lag affects read-your-writes—return `Consistency-` header or document eventual list lag. CAP trade-off explicit in docs.

---

## 44. Backward-compatible addition of `tax_id` to seller profile—walk through REST and proto paths.

**Answer:** REST v2: add optional `tax_id` in JSON schema; old clients ignore; new clients send. No change to URL if additive policy allows. If validation rules require it for new sellers only, use feature flag server-side—not required in schema globally. Proto: add `optional string tax_id = 7`; never reuse field 7 later; run breaking check. Monitor adoption; deprecate v1 when traffic negligible.

---

## 45. Design webhook delivery system API: register endpoint, retries, signatures.

**Answer:** `POST /webhook-subscriptions` with url, events[], secret. On event: POST payload with `X-Signature: HMAC-SHA256(body, secret)`, `X-Delivery-Id`, timestamp. Retry 5xx with exponential backoff; DLQ after N tries; `GET /webhook-deliveries/{id}` for support. Partners verify signature and dedupe by delivery id. Document at-least-once semantics. Rate limit outbound per subscriber to prevent your system DDoSing partners.

---

## 46. API supports 100k RPS read on product catalog—caching and HTTP design?

**Answer:** **GET** by id with cache keys `Cache-Control: public, max-age=60` where safe; ETag/`If-None-Match` for bandwidth. CDN edge cache for anonymous catalog; purge on update via tag or short TTL. Version in URL or cache key on breaking changes. **Avoid** uncacheable POST reads. Origin: read replicas, local in-process cache L1. Monitor hit ratio; stale-while-revalidate for flash sales. Writes go async to search index—see [Chapter 12: Caching](../12-caching-strategies/README.md).

---

## Debugging & Ops

## 47. Partners report intermittent duplicate orders after your deploy. What API-layer checks do you run first?

**Answer:** Idempotency middleware enabled on route? TTL shortened? Key scoped per tenant? Load balancer retrying POST? Compare idempotency hit/replay metrics before/after deploy. Sample duplicate `order_id` pairs—same `Idempotency-Key`? Trace request ids for double execution paths. Rollback if store migration lost keys. Add integration test: client retry simulation on 503.

---

## 48. p99 on `GET /orders` spikes after clients adopt deep `page=` pagination. Diagnosis and fix?

**Answer:** Confirm OFFSET queries in slow query log; explain cursor migration in changelog. Ship `cursor` param; deprecate `page` with sunset. Add index on sort columns. Rate-limit deep offset. Dashboard: latency vs `page` number. Communicate breaking improvement as v2 list endpoint if needed.

---

## Staff+

## 49. Two product lines want different breaking schedules on the same “platform” API. How do you govern versioning org-wide?

**Answer:** Establish **API platform council**: one compatibility policy (additive default, N+1 supported versions, CI breaking checks), shared gateway, and exception process with sunset dates. Product-specific fields via **extensions** or separate BFFs—not forked `/v2` per product without platform sign-off. Metrics on version skew; principal architect owns proto/OpenAPI repo access. Refuse “fast break” without partner impact assessment—offer feature flags and parallel fields first.

---

## 50. Principal review: team proposes “one mega GraphQL endpoint” replacing 20 REST services for all clients. Your response?

**Answer:** Ask requirements: which clients, caching needs, partner ecosystem, team skill, p99 SLO. GraphQL centralizes fan-out but concentrates **blast radius**, authz complexity, and performance risk—N+1 kills without investment. Likely decision: GraphQL **BFF for owned mobile** only; partners stay REST; internal stays gRPC. Require proof-of-concept load test, query cost analysis, and deprecation plan for 20 surfaces—not big-bang. Document trade-offs in ADR; revisit in 6 months with metrics.
