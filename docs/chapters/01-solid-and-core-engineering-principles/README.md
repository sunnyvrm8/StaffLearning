# Chapter 01: SOLID and Core Engineering Principles

> **One line:** SOLID is a vocabulary for *where change should hurt least*—not a license to abstract everything on day one.

## Why this matters in production

A payments team ships a monolith that handles authorization, settlement, fraud scoring, and merchant reporting. After three years, every new card network or fee rule touches the same 4,000-line `PaymentService`. Releases slow down; regressions spike; on-call pages cluster around “we changed one `if` and broke settlement.” Stakeholders feel it as **velocity collapse** and **incident frequency**, not as “we violated the Open/Closed Principle.”

SOLID and adjacent principles (DRY, YAGNI, composition over inheritance, **tell, don’t ask**) give architects a shared language for **separating reasons to change**, **depending on abstractions at boundaries**, and **keeping contracts honest**—so teams can evolve checkout, billing, and integrations without rewriting the core every quarter.

## Core ideas

SOLID names five pressures on object-oriented design. In microservices and modular monoliths, the same pressures appear as **module boundaries**, **API contracts**, and **ownership**:

| Letter | Pressure | Production translation |
|--------|----------|------------------------|
| **S** | One reason to change per module | Split “orchestrate payment” from “persist ledger entry” from “emit analytics event” |
| **O** | Extend behavior without editing stable code | New fee rule = new strategy, not another branch in `calculateFees` |
| **L** | Subtypes honor supertype expectations | A “refundable payment” adapter must not throw where “capture only” succeeded |
| **I** | Clients depend only on methods they use | Don’t force read-only dashboards to implement `write()` on a fat repository |
| **D** | High-level policy doesn’t import low-level detail | `OrderService` depends on `PaymentPort`, not `StripeClient` |

These are **heuristics**, not laws. The architect’s job is to map each letter to **observed change vectors** (new providers, new regulations, new read models)—then decide if indirection pays for itself.

### Single Responsibility (SRP)

**Intuition:** A class or module should answer one question for the business—“who changes this when VAT rules change?”

**Production anchor:** In an order service, mixing **idempotent charge orchestration**, **PDF invoice rendering**, and **CRM sync** in one class means three teams contend for one file. SRP often becomes **separate deployables** or at least packages: `billing-core`, `billing-notifications`, `billing-reporting`.

| | Cohesive module | “God” service |
|---|---|---|
| When | Clear owner, one SLA dimension | Rapid prototype, &lt;3 engineers |
| Risk | Over-splitting → distributed monolith | Every change is a merge conflict |
| Ops signal | Blameless postmortems cite *one* bounded area | Incidents require grep across layers |

**Not:** one public method per class. **Yes:** one *axis of change* per boundary.

### Open/Closed (OCP)

**Intuition:** Stable code should accept new behavior through **extension points** (strategies, plugins, config-driven rules), not endless edits to a central `switch`.

**When it pays:** Fee engines, tax jurisdictions, fraud rules, feature flags for enterprise tiers—domains with **frequent additive change** and **low tolerance for regression** on the happy path.

**When to avoid:** Two variants and no roadmap—OCP via abstract factories for “Visa vs Mastercard” alone is speculative generality (YAGNI).

### Liskov Substitution (LSP)

**Intuition:** Callers of `PaymentProcessor` must not need `instanceof` checks or special cases for subclasses.

**Classic failure:** `Square` overriding `setWidth` breaks `Rectangle` invariants—toy, but the production version is **`RefundingWallet` that throws on partial capture** while the interface promised partial refunds.

**Debugging hook:** Subclass overrides that **strengthen preconditions** or **weaken postconditions** (throws new errors, returns narrower types, silently no-ops). Property-based tests and contract tests on adapters catch LSP breaks early.

### Interface Segregation (ISP)

**Intuition:** Fat interfaces force implementers to stub or lie (`UnsupportedOperationException`).

**Production anchor:** A `BillingRepository` with 20 methods used by one reporting job forces the payments team to implement dead writes. Split **`BillingReader`** / **`BillingWriter`** or use **role interfaces** so consumers compile against what they need.

ISP at service boundaries looks like **narrow gRPC/REST surfaces**—internal admin APIs separate from public checkout APIs (links to [Chapter 09: API Design](../09-api-design/README.md)).

### Dependency Inversion (DIP)

**Intuition:** Policy (business rules) points inward; details (DB, Stripe, S3) point outward—**ports and adapters**.

```
        +------------------+
        |   OrderService   |  policy
        +--------+---------+
                 | depends on
                 v
        +------------------+
        |   PaymentPort    |  abstraction
        +--------+---------+
                 ^ implemented by
        +--------+---------+
        |  StripeAdapter   |  detail
        +------------------+
```

DIP is how you **swap PSPs**, **run contract tests with fakes**, and **keep domain logic testable without WireMock in every unit test**.

### Beyond SOLID: principles that share the shelf

| Principle | One line | Pairing with SOLID |
|-----------|----------|-------------------|
| **DRY** | One authoritative representation of knowledge | Don’t duplicate fee tables; *do* duplicate unrelated DTOs if contexts differ (DDD) |
| **YAGNI** | Don’t build extension points without a second use case | Counterweight to OCP |
| **Composition over inheritance** | Favor has-a over deep is-a trees | Reduces LSP traps |
| **Tell, don’t ask** | Objects enforce invariants internally | Supports SRP and encapsulation |
| **Law of Demeter** | Don’t reach through object graphs | Limits ripple when internals change |

### SOLID vs “clean architecture” vs DDD

| | SOLID (this chapter) | Design patterns (Ch. 2) | DDD (Ch. 3) |
|---|---|---|---|
| Focus | Class/module change pressure | Reusable collaboration structures | Bounded language and ownership |
| Best for | Refactoring a tangled module | Naming solutions in reviews | Splitting services and data |
| Risk | Abstract everything | Pattern fever | Big-bang context map |

Use SOLID **inside** a bounded context; use DDD to decide **where** contexts split.

## When to use / when to avoid

**Use when:**

- Change frequency is high in one dimension (fees, providers, compliance).
- Multiple teams ship against the same codebase area.
- You need test doubles at domain boundaries (payments, identity).
- Incidents trace to “unrelated concern in same class.”

**Avoid when:**

- Prototype or MVP with unknown product shape—**YAGNI beats OCP**.
- Team is two engineers and one deployable—light modules, not 12 interfaces.
- “SOLID refactor” is scheduled without a **change vector**—you get indirection without velocity.
- Framework already owns the extension model (e.g., middleware pipeline)—don’t fight it.

## How it fails

| Symptom | Likely cause | What to check |
|---------|--------------|---------------|
| More interfaces, same merge pain | SRP split along technical layers, not business axes | Who owns changes? Map teams to modules |
| `UnsupportedOperationException` in prod | ISP violation or LSP break on partial impl | Interface usage graph; adapter tests |
| Tests mock 15 dependencies | DIP applied inside domain, not at edges | Constructor injection sprawl |
| Subtle production-only bugs on refunds | LSP: subclass narrows behavior | Contract tests per adapter |
| “Abstraction astronaut” reviews | OCP without second variant | Count real extension points shipped in last 2 quarters |

**Incident pattern:** A new BNPL provider is added; `OrderService` now imports `KlarnaClient` directly. Settlement still works, but **integration tests require live sandboxes**, and a circular dependency appears between `orders` and `payments` packages. Fix: extract `PaymentPort`, move adapter to infrastructure, enforce dependency rule in CI (ArchUnit, import linter).

## Architect takeaway

- **Decide:** Which **change vectors** (provider, regulation, read model, channel) justify ports, strategies, or separate modules in the next two quarters—not forever.
- **Measure:** Lead time for “add fee rule” / “add PSP”; defect rate in touched files; cyclomatic complexity hotspots; dependency cycles between packages.
- **Document in design review:** Explicit **non-goals** (“we will not support N providers until volume justifies OCP”); boundary diagram showing ports; LSP-sensitive operations (refunds, idempotency).

## Diagrams

- [Overview — change pressure on checkout](./diagrams/overview.md)
- [Dependency inversion — ports and adapters](./diagrams/dependency-inversion.md)
- [When to abstract — decision flow](./diagrams/trade-offs.md)

## Code examples

| Scenario | Java | Go |
|----------|------|-----|
| Open/Closed — additive fee rules | [java/OpenClosedFees.java](./java/OpenClosedFees.java) | [go/open_closed_fees.go](./go/open_closed_fees.go) |
| Dependency inversion — payment port | [java/DependencyInversionPorts.java](./java/DependencyInversionPorts.java) | [go/dependency_inversion_ports.go](./go/dependency_inversion_ports.go) |
| Interface segregation — read/write repos | [java/InterfaceSegregationRepository.java](./java/InterfaceSegregationRepository.java) | [go/interface_segregation_repository.go](./go/interface_segregation_repository.go) |

**Production note:** Ship OCP (strategy/registry) when product commits to **N+1 variants** of the same rule family; ship DIP at **integration boundaries** (PSP, ledger, email) from day one in payment systems—even in a monolith—so contract tests and PSP migration stay feasible.

## Related topics

- [Chapter 02: Design Patterns](../02-design-patterns/README.md) — Strategy, Adapter, and ports/adapters as named pattern vocabulary for SOLID refactorings.
- [Chapter 03: Domain-Driven Design and Bounded Contexts](../03-domain-driven-design-and-bounded-contexts/README.md) — Where to draw module and service lines before applying SRP inside a context.
- [Chapter 09: API Design](../09-api-design/README.md) — ISP and stable contracts at HTTP/gRPC boundaries.

## Interview preparation

See [interview-questions.md](./interview-questions.md) (top 20 questions with answers).
