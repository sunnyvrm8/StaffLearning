# Interview Questions: Design Patterns

**Bank size:** 25  
**Rationale:** Medium core chapter (patterns-principles); review vocabulary and legacy evolution.  
**Last updated:** 2026-05-20

---

## 1. Name the three GoF pattern categories and give one production example each (not from textbooks).

**Answer:** **Creational** — how objects are born: a `ReportExporter` factory picks CSV vs PDF builders for merchant statements. **Structural** — how parts compose: an **Adapter** wraps a legacy SOAP billing client behind your `PaymentPort`. **Behavioral** — how collaborators interact: **Strategy** for per-channel notification senders, or **Observer** for `OrderPlaced` handlers inside a modular monolith. Architects name these categories in reviews so the team agrees *which kind of change* is coming (new variant vs new integration vs new workflow step).

---

## 2. What problem does Strategy solve? Contrast with a single `if/switch` on channel type in a notification service.

**Answer:** **Strategy** isolates algorithms that vary independently (SMS throttling vs email template vs push payload) behind one interface (`ChannelSender`); the dispatcher selects an implementation by config or registry. A **switch** is fine for one or two stable channels; it becomes a merge-conflict hotspot when product adds channels, per-region rules, and retry/idempotency differences. Strategy buys **additive change** (new class + register) and focused tests per channel; it costs more types and wiring. The production signal to switch is a committed roadmap of N+1 variants, not “we might add Slack someday.”

---

## 3. What is the difference between Adapter and Facade when integrating a legacy billing API?

**Answer:** **Adapter** translates an *incompatible* foreign interface into *your* domain port—e.g., map SOAP `ChargeRequest` to `PaymentPort.charge(Money, IdempotencyKey)` so `OrderService` never imports legacy types. **Facade** simplifies a *subsystem you own* behind a coarse API—e.g., one `BillingFacade.postOrder()` that orchestrates five internal modules. Use Adapter at the strangler boundary; use Facade when callers are drowning in your own packages, not when you need to hide vendor errors you should surface. Wrong choice: Facade that swallows decline codes and makes reconciliation impossible.

---

## 4. Explain ports and adapters (hexagonal architecture) in one paragraph using a payment or inventory example.

**Answer:** Domain defines **ports** (`PaymentPort`, `InventoryPort`)—what the business needs, not how. **Adapters** in infrastructure implement them (`StripeAdapter`, `WarehouseApiAdapter`). `OrderService` depends only on ports; tests use fakes. Swapping Stripe for Adyen means a new adapter and wiring, not rewriting checkout logic. Inventory example: `ReserveStock` port; adapter calls WMS REST today, Kafka command tomorrow. The win is **test seams and migration**; the cost is indirection—justify when billing or stock is a change vector, not for every CRUD table.

---

## 5. What are the three states of a circuit breaker and what happens to callers in each?

**Answer:** **Closed** — calls pass through; failures accumulate toward a threshold. **Open** — calls fail fast (or take a configured fallback) without hammering the sick dependency; after a wait, **Half-open** — a probe call (or limited traffic) tests recovery; success closes the breaker, failure reopens. Callers must not block the request thread indefinitely: define timeouts, fallback semantics (degrade vs fail closed), and metrics on state transitions. Half-open flapping is an ops signal—thresholds or dependency SLOs are wrong.

---

## 6. How does Observer differ from publishing to Kafka after a transaction commits?

**Answer:** **Observer** is in-process: subject notifies listeners synchronously (or on same thread pool) inside one deployable—low latency, shared memory, but listeners can block the publisher and couple modules. **Kafka after commit** (often via **outbox**) is cross-service: durable, replayable, ordered per partition, but adds latency, consumer lag ops, and idempotency requirements. Use Observer when handlers must see the same transaction boundary or are fast side effects (metrics, in-memory projection). Use outbox + bus when fan-out crosses services, you need replay, or failure must not roll back the publisher’s DB write.

---

## 7. When is Singleton appropriate in a service, and when is it an anti-pattern?

**Answer:** Appropriate for **process-wide scarce resources** with clear lifecycle: connection pool config, breaker registry, or a read-only rules cache loaded at startup—often as a single bean/instance in DI, not a global `getInstance()`. Anti-pattern when it hides **mutable shared state** (request counters, tenant context), complicates testing (order-dependent tests), or masks missing DI. In distributed systems there is no cluster-wide singleton without coordination—prefer injected singletons per process and external stores for shared state.

---

## 8. Sketch a Strategy registry for shipping cost: flat rate, weight-based, and free over threshold. What does the context object pass in?

**Answer:** Define `ShippingStrategy` with `quote(ShipmentContext ctx) -> Money`. Context carries **cart subtotal, total weight, destination zone, merchant id**—everything strategies need without reaching into global config. Registry: `Map<String, ShippingStrategy>` keyed by `strategyId` from merchant settings. `ShippingCalculator` loads strategy id, looks up implementation, returns quote. Flat rate ignores weight; weight-based uses `ctx.weight()`; free-over-threshold checks `ctx.subtotal() >= threshold` then returns zero. Adding a fourth rule = new class + registration, not editing calculator logic.

---

## 9. You inherit a vendor SDK whose types leak into domain code. Outline the Adapter layers you would introduce.

**Answer:** (1) Define domain **port** (`FraudScoringPort.score(OrderSnapshot) -> RiskDecision`) with only domain types. (2) **Adapter** class in `infra` that implements the port, translates domain → SDK request and SDK → domain result (including error mapping). (3) Move all SDK imports behind the adapter; add ArchUnit/import lint: domain cannot reference vendor packages. (4) Contract tests on the adapter against sandbox or recorded fixtures. (5) Optional **anti-corruption** layer if legacy DTOs are gnarly—internal model inside adapter before mapping to domain. Domain never sees `VendorChargeResponse`.

---

## 10. Implement or pseudocode a notification dispatcher that routes by `channel` string to `ChannelSender` implementations without a central switch that grows every sprint.

**Answer:** Registry at startup: `Map<String, ChannelSender> senders` populated from DI (`"email" -> emailSender`, `"sms" -> smsSender`). `dispatch(Notification n)` does `senders.get(n.channel()).orElseThrow().send(n)` or returns a structured “unknown channel” error. New channel = new `ChannelSender` + wire in composition root. For dynamic plugins, load from config listing class names or use a small factory interface. Avoid stringly-typed chaos: validate channel enum at API boundary, keep registry keys in one module. See `java/StrategyNotificationChannels.java` and `go/strategy_notification_channels.go` in this chapter.

---

## 11. A `Template Method` base class for `processRefund()` has hooks `validate`, `capture`, `settle`. When would you refactor hooks to Strategy instead?

**Answer:** Refactor when **steps vary by type more than by skeleton**—e.g., three refund kinds with different validate/settle order, or subclasses override hooks in incompatible ways (LSP risk). Strategy fits **one step, many algorithms** (`RefundValidator`, `SettlementStrategy`) composed by a thin orchestrator. Keep Template Method when the **sequence is regulated and stable** (always validate → capture → settle) and only hook bodies differ slightly. Smell to split: abstract class with many empty overrides, or `if (type == …)` inside hooks.

---

## 12. Production logs show SMS notifications sent twice but email once. List three pattern-related causes in an Observer-style pipeline.

**Answer:** (1) **Duplicate subscription** — two listeners registered for `OrderPlaced` (restart without idempotent registration, or double `@EventListener`). (2) **At-least-once delivery** — retry after timeout though SMS actually sent; no idempotency key on outbound send. (3) **Sync listener + async retry** — publisher retries transaction, Observer fires again; email path dedupes by message id, SMS path does not. Also check: half-open duplicate webhook, or Observer invoked on both “created” and “confirmed” events. Fix with idempotent send keys, single listener registration, and outbox for cross-process fan-out.

---

## 13. Tell me about a time you introduced a pattern during a legacy migration. What would you do differently?

**Answer:** (STAR sketch) Situation: monolith checkout called fraud and billing inline. Task: enable PSP swap without big-bang. Action: introduced `PaymentPort` + `LegacyBillingAdapter`, kept behavior identical, contract tests against legacy; then second adapter for new PSP with shadow traffic. Result: cut migration rollback time, fewer production SDK leaks. **Do differently:** introduce the port **before** the first adapter grows fat with business rules; document **fallback when breaker open** with product sign-off earlier; measure **time-to-add-channel** not just “we used hexagonal.” Lesson: patterns are migration tools—success is lead time and incident rate, not diagram count.

---

## 14. Second notification channel in six months—Strategy now or `switch` until the third channel?

**Answer:** **`switch` is reasonable** if the second channel shares the same contract (retry, idempotency, audit fields) and only payload differs—one well-tested dispatcher with two branches. **Strategy now** if the second channel needs different **operational behavior** (SMS rate limits, partner webhook signatures, separate DLQ) or you already have a registry/feature-flag culture. Third channel on the roadmap is a trigger, not a law—two channels with diverging SLOs often justify extract before the third lands. YAGNI: don’t build Abstract Factory for “email vs SMS” alone.

---

## 15. Circuit breaker on fraud scoring: open breaker returns “allow checkout” vs “block checkout.” What does product/SRE need to decide?

**Answer:** This is **risk appetite**, not an engineering default. **Allow** — prioritizes revenue; accepts fraud loss during outages; requires audit trail, caps, and post-incident review. **Block** — prioritizes loss prevention; may spike cart abandonment; needs comms and manual review queue. SRE needs: fallback metric, breaker open duration alerts, runbook for half-open flapping, and explicit **never silent allow-all** without product sign-off on money at stake. Engineering implements configurable policy, logging of every bypassed score, and shadow mode to measure false positives before choosing.

---

## 16. Decorator for caching vs Proxy for remote calls—same interface, different intent. Give one example of each on an HTTP client.

**Answer:** **Decorator (caching)** — wraps `HttpClient` with `CachingHttpClient`: `get(url)` checks TTL cache before delegating; adds behavior while preserving interface; compose order matters (cache outside retry vs inside). **Proxy (remote control)** — `RemoteInventoryProxy` implements `InventoryPort` but serializes calls over RPC, enforces auth, or lazy-connects; controls *access* to a real object. Mesh sidecars are infrastructure proxies. Interview trap: caching remote GETs is Decorator; stub that looks local but is network is Proxy.

---

## 17. Your team proposes Abstract Factory for two database vendors with no second vendor on the roadmap. How do you respond in a design review?

**Answer:** Challenge the **force**: one vendor, no contract, no multi-tenant isolation need—Abstract Factory adds factory-of-factories and test doubles nobody will use (YAGNI). Prefer **repository on one driver** + port at persistence boundary if you expect *one* swap in 12 months (`OrderRepository` interface, single impl). If compliance mandates second vendor, name the date and **concrete migration test** (replay, dual-write). Offer compromise: thin `DataSource` port now, defer factory hierarchy until second vendor is funded. Track decision in ADR with trigger: “second vendor contract signed.”

---

## 18. In-process Observer vs outbox + message bus for `OrderPlaced`—compare consistency, latency, and operability.

**Answer:** **Observer:** lowest latency; handlers run in-process—use transactional listener or same DB transaction only if failure must roll back order; risk of slow handler blocking checkout. **Outbox + bus:** write event to outbox in same TX as order, async publish—**at-least-once**, higher latency, independent scaling, consumer lag/DLQ ops, mandatory **idempotent** consumers. Consistency: Observer can be effectively exactly-once in one DB; bus needs dedup keys. Operability: bus gives replay and cross-team ownership; Observer needs tracing through listener chains. Rule: cross-service fan-out or replay ⇒ outbox; same-module side effects with strict TX ⇒ in-process or transactional outbox only.

---

## 19. Checkout calls fraud (50ms p50, 2s p99) and inventory (20ms). Where do timeout, retry, and circuit breaker apply on each?

**Answer:** **Fraud** — aggressive **timeout** below user-facing SLA (e.g., 300–500ms), **limited retry** only on idempotent read/score with jitter (not blind retry on POST), **breaker** when error rate or latency SLO breaches—fallback per product policy. **Inventory** — shorter timeout (fail fast on stock hold), **retry** on transient 503 with idempotent reserve if API supports it, breaker if warehouse is down to avoid thread pile-up. Do not retry non-idempotent holds without reservation tokens. Never share one breaker for both—**bulkhead** per dependency. Inventory is often on critical path; fraud may be async shadow on some merchants.

---

## 20. A strangler migration exposes the same `PaymentPort` to Stripe and a legacy mainframe adapter. What tests and traffic-shifting controls do you require?

**Answer:** **Contract tests** on `PaymentPort` for both adapters (idempotency, partial capture, decline mapping). **Shadow or canary** — route % traffic to new adapter with compare of amount, currency, provider ref. **Reconciliation** job matching ledger to provider files. **Feature flag** per merchant to rollback instantly. **Idempotent charge keys** end-to-end. Load tests on mainframe adapter latency. Do not “flip DNS”; require error-budget gate and runbook. Dual-write only with clear source of truth and diff alerts.

---

## 21. Circuit breaker metric shows OPEN but checkout latency is still high. What four non-obvious causes do you investigate?

**Answer:** (1) **Breaker on wrong dependency** — open on fraud but slowness is inventory or DB. (2) **Fallback path is expensive** — “fail open” still calls a backup ML model or manual queue synchronously. (3) **Half-open probe storm** — many instances probe simultaneously, amplifying load. (4) **Timeout > breaker** — threads still block until HTTP timeout while metric says OPEN on a different client bean. Also: thread pool exhaustion from queued calls, retry loops bypassing breaker, or breaker open but **cached stale “allow”** path doing heavy work. Correlate trace spans with breaker state per dependency.

---

## 22. A senior engineer labels every new class a “pattern” in PRs. How do you raise the bar without shutting down discussion?

**Answer:** Ask **which force** the pattern addresses (varying algorithm? foreign API? failure isolation?) and what happens if we use a function + config instead. Praise naming when it speeds reviews (“this is Adapter at the port”); redirect when it’s **speculative complexity** (“Singleton” for stateless helpers). Team norm: link to a one-page **pattern decision checklist** in the handbook; RFC template has “simplest alternative rejected because…”. 1:1 with curiosity, not gatekeeping—goal is shared vocabulary, not badge collecting. Measure PRs that reduce change vectors, not pattern count.

---

## 23. Design a State pattern for a subscription lifecycle (trialing → active → past_due → canceled) vs encoding state in strings and scattered `if`s.

**Answer:** Each state implements `SubscriptionState` with `activate()`, `markPastDue()`, `cancel()`—illegal transitions throw or return `InvalidTransition`. Context `Subscription` holds current state object; transitions replace state (`sub.setState(new ActiveState())`). vs string + `if`: strings invite **invalid combos** (`"canceled"` but still billing), scattered guards, and untested branches. State shines when **transitions and side effects differ** (trial end starts billing; past_due sends dunning). For two states, enum + methods may suffice. Add persistence mapping state id ↔ object factory on load.

---

## 24. Platform team offers a shared circuit-breaker library with default thresholds. Service teams report flapping breakers. What governance and tuning process do you establish?

**Answer:** Ban one global threshold. **Defaults per dependency class** (optional enrichment vs payment capture) with **mandatory override** documented in service config. Dashboards: open/half-open duration, call volume rejected, comparison to dependency p99. **Tuning workflow:** post-incident review adjusts thresholds; services propose overrides via PR to platform values.yaml. Governance: new dependency must register breaker name, owner, fallback policy, and SLO. Platform provides instrumentation and libraries; **teams own SLO fit**. Flapping often means timeout too short or half-open probe count too high—fix with metrics, not “increase threshold blindly.”

---

## 25. Map Strategy, Adapter, ports, and circuit breaker to SOLID letters for a checkout refactor touching PSP, fees, and fraud—what order do you introduce them and why?

**Answer:** **DIP + Adapter at PSP first** — `PaymentPort` and legacy/new adapters; biggest test and strangler win, stops SDK leak. **OCP + Strategy for fees/channels second** — additive product rules without touching orchestration. **Breaker on fraud last** — optional path; requires explicit fallback policy and product sign-off; depends on stable port boundary. Order follows **change vectors and risk**: revenue path correctness before enrichment resilience. Not “patterns alphabetically.” YAGNI: no Abstract Factory until second vendor is real. Tie to metrics: adapter parity tests, fee lead time, checkout p99 during fraud outages.
