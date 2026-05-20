# Interview Questions: Go Deep Dive

**Bank size:** 50  
**Rationale:** Core handbook chapter — dual-stack Staff+ credibility; emphasizes runtime, concurrency, errors, and production debugging parallel to the Java bank.  
**Last updated:** 2026-05-20

---

## Foundations

## 1. What executes Go source, and how does that differ from shipping a JAR?

**Answer:** `go build` produces a **static binary** (by default) linking the runtime—no separate VM install on the host. Deployment is copy binary + config. Trade-off: rebuild per OS/arch; JVM ships bytecode + shared runtime tuning. Cold start is usually faster; perf tuning uses `GOGC`, `GOMAXPROCS`, not `-Xmx`.

## 2. Explain the Go memory model in terms visible to service authors.

**Answer:** The spec defines **happens-before** via channel ops, `sync` primitives, and `atomic`. Without synchronization, goroutines can see stale reads. Prefer passing data **through channels** or protecting with `mutex`—don’t share structs without a clear owner.

## 3. What is a goroutine, and why is “just spawn one per request” dangerous at 10k RPS?

**Answer:** A goroutine is a lightweight concurrent task scheduled on OS threads. At 10k RPS, unbounded spawns mean **millions of goroutines**, scheduler overhead, and memory for stacks. Bound with **worker pools**, semaphores, or `errgroup` with limits; size from downstream capacity.

## 4. Buffered vs unbuffered channels—when does each apply?

**Answer:** **Unbuffered** synchronizes sender/receiver (handoff). **Buffered** decouples up to capacity—use for bounded queues with known backlog. Unbuffered on hot paths can **block producers** unexpectedly; buffered without consumers leaks memory when full.

## 5. What does `GOMAXPROCS` control?

**Answer:** Max OS threads executing Go code simultaneously—defaults to CPU count. Raise cautiously on I/O-heavy workloads; lowering can reduce contention on some hosts. It does **not** cap goroutine count. Profile before tuning in production.

## 6. How does Go handle errors differently from Java exceptions?

**Answer:** Errors are **values** returned alongside results; callers must check. Panics are for **bug-level** failures (nil deref), recovered only at process boundaries (HTTP middleware). No stack trace unless logged. Architects enforce **wrap with context** (`fmt.Errorf("charge: %w", err)`) and map at API edge.

## 7. Explain `errors.Is` and `errors.As` vs string comparison.

**Answer:** `errors.Is` walks the chain for sentinel errors (`io.EOF`, `sql.ErrNoRows`). `errors.As` extracts typed errors for branching retry policy. String matching breaks with wrapping. Libraries should export **sentinel or typed errors** document retryability.

## 8. What is the zero value philosophy, and one pitfall?

**Answer:** Types default to **usable zero** (`0`, `""`, `nil` slices illegal to append—use `make`). Pitfall: `nil` map write panics; `nil` interface holding typed nil is not `== nil` in some JSON cases—know interface semantics.

## 9. How do Go generics (1.18+) change API design?

**Answer:** Write **type-safe** containers and helpers (`Map`, `Slice` utils) without `interface{}` casts. Constraints use interfaces or `comparable`. Don’t generic-ify everything—prefer simple functions until duplication hurts. Compile time cost increases slightly.

## 10. What is an interface in Go—implicit satisfaction?

**Answer:** Types implement interfaces **implicitly** by method set—no `implements` keyword. Small interfaces (`io.Reader`, `io.Writer`) compose well. Pitfall: **large interfaces** (`Database`) are hard to mock and evolve—split by consumer (ISP at Go scale).

## 11. `make` vs `new`—difference?

**Answer:** `new(T)` allocates zeroed memory, returns `*T`. `make` initializes **slices, maps, channels** with length/capacity. Slices need `make([]T, 0, cap)` for append efficiency. Maps need `make` before write.

## 12. What is escape analysis, and why should architects care?

**Answer:** Compiler decides if values live on **stack or heap**—affects GC pressure. Hot paths allocating large objects per request increase GC CPU. Use `go build -gcflags=-m` in dev to study escapes; prefer pointers only when needed.

---

## Application

## 13. How does `context.Context` propagate through a microservice tree?

**Answer:** Incoming HTTP/gRPC handlers derive child contexts with **timeout/cancel**; pass `ctx` as first param to all downstream calls. On shutdown, root cancel drains work. Missing `ctx` breaks **deadline alignment**—client gone but server still charges PSP.

## 14. Standard library HTTP server vs frameworks (Gin, Echo)—trade-off?

**Answer:** `net/http` is production-grade with **middleware chaining** via wrappers—fewer magic defaults. Frameworks add routing sugar, validation, binding—faster dev, watch dependency weight. Staff services often use `http` + small internal kit for consistency.

## 15. How do you structure a Go repo for a modular service?

**Answer:** `cmd/service/main.go` thin; `internal/` packages by domain (`billing`, `adapters`); `pkg/` only if truly reusable libraries. Enforce **import rules** (no `internal` cross-service). `go mod` pins deps; CI runs `govulncheck`, `staticcheck`, race detector.

## 16. gRPC in Go—what must every client set?

**Answer:** **Dial options**: TLS, keepalive, max message size, and **per-RPC timeouts** via `context`. Use interceptors for metrics and auth. Handle `status.FromError` for codes—retry only idempotent ops on `Unavailable`. Connection pooling via shared `ClientConn`.

## 17. `database/sql` connection pool—how do you size it?

**Answer:** `SetMaxOpenConns`, `SetMaxIdleConns`, `SetConnMaxLifetime` from DB capacity and instance count. Too many conns across 100 pods exhaust Postgres. Watch **wait duration** on pool stats. Use `QueryContext` always.

## 18. When is `sync.Pool` appropriate?

**Answer:** Reuse **temporary allocations** (byte buffers, encoded objects) to reduce GC—not a cache of business objects (objects may be freed anytime). Clear pooled items before `Put`. Wrong use: storing state you need later—data races and surprises.

## 19. How do you load configuration in twelve-factor Go services?

**Answer:** Env vars via `os.Getenv` or libraries like `caarlos0/env`; secrets from vault/sidecar—not committed files. Struct validation at startup **fail fast**. Support feature flags as config with reload only if designed idempotently.

## 20. What does the race detector cost, and when run it?

**Answer:** ~5–10× CPU/memory overhead—run in **CI on tests**, not prod. Catches unsynchronized map writes and data races on shared structs. Mandatory for concurrent payment/idempotency code paths.

## 21. How do you expose metrics and pprof safely?

**Answer:** Prometheus `/metrics` on admin port or separate listener; **auth network policy**. `pprof` on `:6060` bound to localhost or debug VPN—never public internet. Sample CPU during incidents; block profile for goroutine leaks.

## 22. Go modules: what breaks reproducible builds?

**Answer:** Missing `go.sum`, `replace` to local paths, pseudo-versions without tags, retracted modules ignored. CI should `go mod verify` and vendor optionally for air-gap. Pin tool versions (`go install tool@version`).

---

## Coding

## 23. Implement graceful shutdown for an HTTP server.

**Answer:** Listen for `SIGINT/SIGTERM`; call `srv.Shutdown(ctx)` with timeout context; stop accepting; wait in-flight requests; cancel background workers. `http.Server` `BaseContext` ties to root context. Exceed timeout → log and exit.

## 24. Merge two sorted slices of events by timestamp.

**Answer:** Two indices i,j; append smaller timestamp; O(n+m) time O(1) extra if reusing output slice with cap. Stable tie-break on ID. For disk scale, k-way merge.

## 25. Write a function to detect duplicate idempotency keys from a channel stream.

**Answer:** `seen := map[string]struct{}{}`; on receive, if exists return duplicate else insert. For cluster-wide dupes use **Redis SETNX** or DB unique index—not in-memory alone.

## 26. Implement worker pool processing jobs with context cancel.

**Answer:** N worker goroutines read `jobs` channel; main sends jobs; on `ctx.Done()` stop sending, close jobs, wait `sync.WaitGroup`. Workers select on ctx in long tasks. Don’t leak goroutines—wait for drain.

## 27. Parse a 10GB log file for ERROR lines without loading all.

**Answer:** `bufio.Scanner` line loop; optional `scanner.Buffer` for long lines. Pipeline filters to channel with bounded buffer. Don’t `ReadAll`. Parallelize by byte offsets only if line boundaries handled.

## 28. Rate limit per API key with `golang.org/x/time/rate`.

**Answer:** `map[string]*rate.Limiter` with mutex; `Allow()` or `Wait(ctx)` per request. Evict stale keys with LRU to cap memory. Return 429 when denied; document burst.

## 29. Why might `defer` in a tight loop hurt performance?

**Answer:** Each `defer` has overhead; in hot loops inline cleanup or batch defers outside loop. `defer` is fine in HTTP handlers for **close body, unlock mutex**—clarity wins at request granularity.

## 30. Implement retry with exponential backoff for idempotent GET.

**Answer:** Loop with `attempt`, cap `max`, jitter `sleep`; respect `ctx`; stop on non-retryable errors (`errors.As` for 4xx). Use `github.com/cenkalti/backoff/v4` in prod for tested policies.

## 31. Safely append to a slice from multiple goroutines?

**Answer:** Don’t—**race**. Use mutex around slice, channel aggregation, or pre-partition work per goroutine then merge. `sync.Map` rarely for append patterns; collect results channel.

## 32. Write table-driven tests for a fee calculator—why idiomatic?

**Answer:** `tests := []struct{name, input, want}{...}` loop `t.Run(tt.name, func(t *testing.T){...})` — clear cases, parallel subtests optional. Documents examples for reviewers; easy add edge case without new function.

---

## Design & Trade-offs

## 33. Go service behind Java orchestrator—contract essentials?

**Answer:** Protobuf/OpenAPI; **timeouts on both sides**; idempotency keys; shared trace headers; explicit **error code mapping**. Avoid leaking Go-specific strings; Java side uses generated stubs with same deadlines.

## 34. When are channels better than mutexes?

**Answer:** Channels when **ownership transfers** or you model pipelines/backpressure. Mutex when protecting a small shared struct’s fields (cache map, counters). “Share memory by communicating” doesn’t mean all state goes through channels.

## 35. Should you use an ORM (GORM) in Go?

**Answer:** GORM speeds CRUD prototypes; costs **magic SQL, N+1, migration pain**. Many Staff teams prefer **`sqlc` or hand SQL** for ledger paths. Decide per bounded context—billing core may forbid ORM; read models may use it.

## 36. Monolith binary vs many Go microservices?

**Answer:** Single binary with `internal` packages until **independent scale/team** boundaries clear. Go’s fast compile favors monolith; ops cost is **service count**, not binary count. Extract when SLO or ownership demands.

## 37. `interface{}` / `any` at boundaries—policy?

**Answer:** Acceptable at **JSON decode, plugin hooks**—narrow immediately to typed structs. Ban `any` in domain core—defeats generics benefits and hides bugs until runtime.

## 38. How do you version a shared Go module used by 30 services?

**Answer:** Tag **semver**; avoid breaking changes without major; use `go get -u` in CI with tests. Document deprecations. For internal modules, **monorepo + workspace** or Artifactory proxy with retention policy.

## 39. Error wrapping policy across packages?

**Answer:** Wrap at boundaries with context; don’t wrap **sentinel** repeatedly losing `Is`. Log once at top handler; lower layers return wrap. `%w` only. Map to HTTP status in one place.

## 40. Pick Go vs Java for a new fraud rules engine?

**Answer:** Go if **low footprint, simple rules, high fan-out I/O** to feature store. Java if **complex rules DSL, heavy integration with enterprise data grid, team depth Java**. Either works—pick team and library maturity; enforce test harness for rules.

## 41. Global variables in Go services—acceptable?

**Answer:** Package-level **metrics, default logger, compiled regex** OK if init controlled. Mutable globals (`var cache map`) are incident bait—inject dependencies via `main` struct. `init()` order surprises—minimize.

## 42. How does Go’s lack of inheritance shape domain modeling?

**Answer:** Use **composition + small interfaces**; embed structs for reuse without fragile hierarchies. State machines as explicit types + methods, not deep subclass trees—aligns with payment state modeling.

---

## Debugging & Ops

## 43. Goroutine count climbing over days—how do you debug?

**Answer:** `curl /debug/pprof/goroutine?debug=2` or equivalent; look for **stuck stacks** on channel receive or HTTP client without timeout. Fix leak: ensure ctx cancel, close bodies, exit worker loops. Add metric `go_goroutines`.

## 44. High GC CPU at moderate heap—levers?

**Answer:** Reduce allocations (reuse buffers with `sync.Pool`, fewer JSON allocs via codegen), tune `GOGC` (higher = less frequent GC, more heap), profile heap with `pprof`. Check for **accidental string concat in loops**.

## 45. `fatal error: concurrent map read and map write` in prod?

**Answer:** Classic missing mutex or using map from goroutines without sync. Fix with `sync.RWMutex` or `sync.Map` for read-heavy caches. Race detector should have caught in CI—add test covering concurrent path.

## 46. TLS handshake timeouts from Go gateway to Java upstream?

**Answer:** Check **connection reuse** (`Transport.MaxIdleConns`), DNS TTL, cert expiry, SNI. Align timeouts: gateway dial < client deadline. Trace with OpenTelemetry net/http spans.

---

## Staff+

## 47. Team wants `panic` for business errors—“invalid amount.” Response?

**Answer:** Reject—panics skip normal control flow and crash requests unless recovered. Return `error` with typed `ErrInvalidAmount`; map to 400. Reserve panic for programmer bugs; metric panic recoveries separately.

## 48. Code review: colleague uses `time.Sleep` in retry loop without jitter.

**Answer:** Request **full jitter** backoff to prevent thundering herd on recovering dependency; respect `ctx.Done()` during sleep. Point to shared retry helper and SLI-based max attempts.

## 49. Principal scope: 200 Go services, inconsistent observability—three mandates?

**Answer:** (1) **OTel SDK + required span attributes** on inbound/outbound, (2) **lint/check** blocking `context.Background()` in handlers, (3) **SLO dashboards** template per service. Measure coverage % in catalog scorecard.

## 50. Executive asks “rewrite Java monolith to Go for cost.” Your response?

**Answer:** Cost often lives in **over-provisioned instances, DB chatter, and missing caches**—language swap rarely 10× saves without rewrite risk. Propose **profile Java**, optimize hot paths, use Go for **new stateless edges** where image size matters; only replatform core if team and SLA evidence support multi-year investment.
