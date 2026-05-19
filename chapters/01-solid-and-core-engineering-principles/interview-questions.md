# Interview Questions: SOLID and Core Engineering Principles

**Top 20** with answers — for quick review and mock drills.  
**Last updated:** 2026-05-19

---

## 1. State the five SOLID principles in one sentence each, using a checkout/payments example.

**Answer:** **S** — `OrderService` orchestrates placement; fee calculation and PSP calls live in separate modules so a tax change does not touch Stripe wiring. **O** — New fee types register as strategies instead of editing a central `switch`. **L** — Any `PaymentProcessor` implementation must honor the same refund/capture contract callers expect. **I** — Dashboards depend on `InvoiceReader`, not a 20-method repository they half-implement. **D** — Domain code depends on `PaymentPort`; `StripeAdapter` sits in infrastructure.

---

## 2. What is Single Responsibility Principle actually optimizing for?

**Answer:** One **axis of change** per module (who changes this when VAT rules change?), not “one method per class.” In production, SRP often becomes separate packages or services when merge conflicts, incident blast radius, or team ownership lines up with business concerns—not arbitrary file splitting.

---

## 3. How does Dependency Inversion differ from “using dependency injection”?

**Answer:** **DIP** is a design rule: high-level policy depends on abstractions; details (SDKs, DB drivers) implement them and point inward. **DI** is a mechanism (constructor injection, frameworks) to supply implementations. You can inject a `StripeClient` everywhere and still violate DIP if domain imports Stripe types. DIP is satisfied when `OrderService` only knows `PaymentPort`.

---

## 4. Explain Liskov Substitution with a refund scenario—not Rectangle/Square.

**Answer:** If `PaymentProcessor.refund(partialAmount)` is defined for all processors, a subclass that throws on partial refunds breaks callers that worked with the base type. Callers must not need `instanceof` or special cases. Fix: split interfaces (`RefundablePayment` vs `CaptureOnlyPayment`), favor composition, or enforce behavior with contract tests on every adapter.

---

## 5. Where does Interface Segregation show up at an HTTP/gRPC boundary?

**Answer:** A single “Billing API” with admin writes, merchant reads, and public checkout forces every client to see operations it must not use—or teams ship `501` stubs. Split surfaces: `CheckoutBillingAPI` (charge, idempotent pay), `MerchantReportingAPI` (read-only aggregates), internal `SettlementAPI` (writes). Same ISP idea as `InvoiceReader` vs `LedgerWriter` in code.

---

## 6. How do YAGNI and Open/Closed tension in week one of an MVP?

**Answer:** **YAGNI wins** until a second real variant is committed (second PSP, second fee jurisdiction, second tenant rule family). Ship the simplest thing that meets today’s requirements; add a registry or port when the roadmap item has a date and owner—not because SOLID might matter someday. Premature OCP adds indirection without shortening lead time.

---

## 7. One PSP today, three in twelve months—introduce `PaymentPort` now or later?

**Answer:** Introduce a **thin port now** if billing is core revenue and PSP migration is on the roadmap: domain stays testable, adapters stay isolated. Defer only if the team is tiny, one deployable, and no migration for 18+ months—then document the trigger (“second PSP contract signed”) so you do not embed SDK calls in domain for years. Align with product on the migration date, not architecture taste.

---

## 8. A `PaymentService` is ~5k lines. How do you decompose without jumping to fifteen microservices?

**Answer:** Split by **business reason to change** inside the monolith first: `PaymentOrchestration` (charge, idempotency), `FeeEngine` (rules), `LedgerWriter` (persistence), `RefundService`, `ProviderAdapters` (infra). Enforce package dependency rules (domain → ports ← infra). Extract a service only when independent scaling, failure isolation, or team ownership demands it—not because the class is large.

---

## 9. Configurable merchant fee rules: strategy registry, rules engine, or `switch`?

**Answer:** **`switch` / config table:** few rules, rare changes, one team—fastest. **Strategy registry (OCP):** many additive rule types, same evaluation pipeline, engineers ship new `FeeRule` classes—good default for product-led fee experiments. **Rules engine (buy/build):** legal/compliance needs business-owned DSL, non-engineers edit rules, audit trails—justify cost and ops. Avoid a registry for two fee types (YAGNI).

---

## 10. Sketch an OCP-friendly design for `feeType` instead of a growing `switch`.

**Answer:** Define `FeeRule` with `id()` and `apply(subtotal, context)`. Register rules in a `Map<String, FeeRule>` built at startup or from config. `FeeCalculator.total(ruleId, …)` looks up the rule—new types add a class and registration, no edit to calculator logic. See `java/OpenClosedFees.java` and `go/open_closed_fees.go` in this chapter.

---

## 11. `WalletPayment` extends `Payment` but `refund()` is a no-op. How do you fix it?

**Answer:** That violates **LSP**—callers expect refunds to work. Prefer **composition**: `Payment` + optional `RefundCapability` interface, or separate types `CapturablePayment` and `RefundablePayment`. Never inherit to reuse code if the subtype narrows behavior. Add contract tests: “every production adapter implementing `RefundablePayment` must pass partial-refund cases.”

---

## 12. `ReportingService` only needs `listOpen()` but depends on full CRUD. Refactor?

**Answer:** Depend on **`InvoiceReader`** with `listOpen()` and `findById()` only; settlement workers get **`LedgerWriter.append()`**. Reporting never sees write methods; no `UnsupportedOperationException` stubs. At scale, the same split becomes read APIs vs write APIs (CQRS-lite).

---

## 13. A god class constructs twelve concrete clients in its constructor. DIP refactor steps?

**Answer:** (1) List **integration boundaries** (PSP, email, ledger, fraud). (2) Introduce **ports** per boundary used by domain. (3) Move SDK construction to **adapter** types in an `infra` package. (4) Inject ports via constructor; wire in composition root / main. (5) Add **arch lint** (no `domain` → vendor imports). (6) Replace live clients in tests with **fakes** per port. (7) Delete dead methods from the god class as callers migrate.

---

## 14. Fraud scoring and payment capture share one DB transaction. Split or keep together?

**Answer:** **Keep one transaction** if you need atomic “decline fraud ⇒ never capture” and can tolerate coupling and longer locks. **Split** when fraud is slow/ML-heavy or owned by another team: orchestrate with **sagas**, idempotent capture, compensating voids, and clear failure states. SRP suggests separate modules either way; **consistency requirements** decide sync transaction vs async workflow—not SOLID alone.

---

## 15. Enforce “domain must not import vendor SDKs” in CI—what does that look like?

**Answer:** Java: **ArchUnit** `noClasses().that().resideInPackage("..domain..").should().dependOnClassesThat().resideInAnyPackage("com.stripe..")`. Go: **import rules** or `go list` + custom linter forbidding `domain` → `internal/stripe`. Fail the build on violation; allow-list only generated API stubs if unavoidable. Pair with code review: new SDK imports only in `infra/`.

---

## 16. Strangler migration off a legacy `PaymentService`—what order of extraction?

**Answer:** (1) **`PaymentPort` + adapter** wrapping legacy behind the port (no behavior change). (2) **Read-only paths** (status, receipts) to new modules. (3) **Idempotent charge** on new orchestrator with shadow/dual-write compare. (4) **Refunds** after reconciliation proves parity. (5) **Fee/rules** once charge path is stable. (6) Decommission legacy when traffic and error budgets match. Never big-bang; feature-flag each slice.

---

## 17. Zero-downtime PSP migration (e.g. Stripe → Adyen)—outline.

**Answer:** Domain already on **`PaymentPort`**. Add **AdyenAdapter**; route with feature flags by merchant or %. Use **idempotency keys** on all charges. Run **shadow** charges or parallel auth on a slice; **reconcile** provider refs and amounts. Shift traffic gradually; keep Stripe adapter for rollback until mismatch rate is near zero. Monitor decline codes, latency p99, and reconciliation gaps—not just error rate.

---

## 18. Leadership wants “SOLID everywhere” before international launch. What roadmap do you propose?

**Answer:** Sequence by **change vectors**, not letters: Month 1–2 — **DIP** at PSP + ledger boundaries and CI dependency rules. Month 2–3 — **SRP** split of billing monolith hotspots (fees, orchestration). Month 3–4 — **OCP** only for fee/tax plugins with sandbox + contract tests. Month 4–6 — **ISP** on public vs admin APIs. Defer abstract factories. Track **lead time for fee/PSP changes** and **change failure rate**, not interface count.

---

## 19. `UnsupportedOperationException` from a repository implementation—what went wrong?

**Answer:** Usually **ISP** (fat interface, skinny impl) or **LSP** (subclass throws where parent worked). Trace which caller invoked the method; split **Reader/Writer** interfaces or split services. Add static checks so reporting code cannot compile against write APIs. In incidents, grep for `UnsupportedOperationException` and map to the fat interface that should be narrowed.

---

## 20. Contract tests between `OrderService` and a PSP adapter—who owns what?

**Answer:** **Consumer** (`OrderService` team): tests against **`PaymentPort`** using a fake or recorded fixtures; asserts idempotency, error mapping, timeout behavior the domain needs. **Provider** (adapter/infra team): tests against **PSP sandbox** or wire mocks proving request/response mapping. **Shared contract artifact** (Pact, OpenAPI examples): both sides verify; run in CI on adapter and domain PRs. Prevents LSP breaks when one team changes refund semantics without the other knowing.
