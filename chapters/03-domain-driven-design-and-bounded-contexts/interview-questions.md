# Interview Questions: Domain-Driven Design and Bounded Contexts

**Top 10** with answers — for quick review and mock drills.  
**Last updated:** 2026-05-20

---

## 1. What is a bounded context in one sentence, with a marketplace example?

**Answer:** A **bounded context** is a boundary where a domain model and its **ubiquitous language** are internally consistent. In a marketplace, **checkout** has `Cart` and `PlaceOrder`; **fulfillment** has `Shipment` and `PickList`—both might say “order” in conversation, but they are **different models** linked by integration (events/API), not one shared `orders` row with overloaded `status` values.

---

## 2. How is a bounded context different from a microservice?

**Answer:** A **bounded context** is a **model and ownership** boundary; a **microservice** is a **deployable** boundary. You can run **multiple contexts in one monolith** (separate packages/modules) or **one context split across services** (usually a mistake). Split services when **scaling, release cadence, or failure isolation** justify network cost—not because DDD drew a box. The context map should exist even if you ship one JAR.

---

## 3. What is ubiquitous language, and how does it fail in production?

**Answer:** **Ubiquitous language** means engineers, product, and ops use the **same terms** for concepts **within a context**—reflected in code (`PaymentCaptured`), events, and runbooks. It fails when teams share a database but **not** a glossary: checkout’s `READY` means “payment method selected” while fulfillment hears “ready to pick.” Fix: rename to precise terms (`PaymentCaptured`), per-context glossaries, and **ACL** at integration points—not one enterprise spreadsheet nobody reads.

---

## 4. What is an aggregate, and how do you choose aggregate boundaries?

**Answer:** An **aggregate** is a cluster of domain objects treated as **one unit for consistency**, with a **root** that enforces invariants. Choose boundaries where **business rules must be atomic** in the happy path (e.g., `Order.submit()` ensures non-empty lines and locks edits). Keep aggregates **small** to reduce contention; cross-aggregate rules use **events or sagas** with eventual consistency. If two teams always argue about the same transaction, you may have drawn the aggregate wrong—or the context boundary.

---

## 5. When do you introduce an anti-corruption layer (ACL)?

**Answer:** When an **upstream or legacy model** would otherwise leak into your context—legacy WMS codes, partner JSON, or another team’s DTOs. The ACL **translates** external shapes into your domain types and errors at the boundary (often behind a port). Skip ACL only when you control both sides and the contract is already expressed in your language. Symptom without ACL: domain imports `com.vendor.LegacyPickResponse` and every vendor change rewrites business logic.

---

## 6. Explain three context-map relationships and when you’d use each.

**Answer:** **Customer–supplier:** upstream (checkout) publishes `OrderPlaced`; downstream (fulfillment) depends on that contract—document versioning and idempotency. **Anti-corruption:** downstream translates upstream or legacy (billing maps checkout order → invoice lines; fulfillment ACL to WMS). **Conformist:** downstream accepts upstream’s model when translation cost outweighs benefit (analytics consuming checkout events as-is). **Shared kernel:** share a **tiny** stable type (`Money`)—avoid sharing `Order` entities across contexts.

---

## 7. The same database has `customers` used by billing and support. Is that one bounded context?

**Answer:** **Not necessarily**—a shared database is often **technical coupling**, not proof of one model. Billing’s **“customer”** (payer, tax ID, payment methods) differs from support’s **“customer”** (tickets, SLA tier). Treat them as separate contexts with **different models**; integrate via APIs/events. If you must share a DB during migration, enforce **schema ownership** per context (views, separate schemas) and plan extraction—otherwise you get silent conformism to the worst legacy schema.

---

## 8. How would you discover bounded contexts before a microservices split?

**Answer:** Combine **event storming** or domain workshops (nouns, verbs, policies), **organizational ownership** (who changes what when regulations shift), and **write patterns** (who owns authoritative state). Look for **language forks**—same word, different rules. Deliver a **context map** with integration style per arrow, start with **modular monolith** seams, extract the first service where pain is highest (deploy conflicts, scale, incidents)—not a big-bang cut by entity table.

---

## 9. Checkout must charge payment and create a shipment. One transaction or two?

**Answer:** **Two aggregates / two contexts** in the general case: `Order` submit in checkout and `Shipment` creation in fulfillment are not one ACID transaction across services. Use **domain events** (`OrderPlaced`, `PaymentCaptured`) plus **outbox**, **idempotent consumers**, and a **saga** or process manager for failure compensation ([Chapter 23](../23-idempotency-sagas-and-distributed-transactions/README.md)). Reserve **single transaction** for **one aggregate** inside one context (e.g., cart + lines on submit). Interview red flag: “2PC across checkout and warehouse” without latency and ops trade-offs.

---

## 10. When should a team *not* invest heavily in DDD?

**Answer:** Skip heavy DDD when the domain is **simple CRUD**, one small team, stable language, and no legacy translation pain—use clear modules and YAGNI. Also avoid **big upfront context maps** with no shipping path, **anemic domain models** (entities as bags of fields, logic in “services”), or DDD as **mandate to microservice everything**. Invest when **model mismatch** causes incidents, expensive cross-team schema changes, or regulated invariants need clear ownership. Principals optimize for **outcomes**, not ceremony.
