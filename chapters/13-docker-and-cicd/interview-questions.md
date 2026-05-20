# Interview Questions: Docker and CI/CD

**Bank size:** 10  
**Rationale:** Medium ship-mechanics chapter; rubric 10 for pipeline-focused drill set.  
**Last updated:** 2026-05-20

---

## Core

## 1. What is the difference between an image and a container in operational terms?

**Answer:** An **image** is an immutable, versioned artifact (layers + manifest)—what you build once and promote dev → staging → prod. A **container** is a **running instance** of that image with writable layer, cgroup, and namespace isolation. Pain: “works on my machine” when prod runs `:latest` instead of digest `sha256:…`. Architect rule: deploy **immutable tags/digests**, never rebuild at deploy time without recording the same inputs.

---

## 2. Why do multi-stage Dockerfiles matter for Java/Go services?

**Answer:** **Build stage** carries compilers, dev dependencies, and source; **runtime stage** copies only the JAR/binary and minimal base (e.g., `distroless`, `alpine` + ca-certs). Smaller images → faster pull on K8s scale-out (~seconds per node at 100+ pods), smaller attack surface, fewer CVEs to patch. Trade-off: slightly more complex Dockerfile vs. 800 MB fat JRE images that slow deploys during incidents.

---

## 3. Walk the stages of a typical CI pipeline for a microservice—from commit to deployable artifact.

**Answer:** (1) **Checkout** pinned commit. (2) **Lint/static analysis** fast fail. (3) **Unit tests** parallelized. (4) **Build** (Maven/Gradle, `go build`). (5) **Integration/contract tests** with Testcontainers or mocks. (6) **Image build** + scan (Trivy/Snyk). (7) **Push** to registry with tag `git sha`. (8) **Sign/provenance** (optional SLSA). (9) **Deploy** via GitOps/helm with that tag—not “build on prod server.” Target: main branch **<15–30 min** feedback; artifact promotion separates **build once, deploy many**.

---

## 4. Compare blue/green, rolling, and canary deployments for a payment API.

**Answer:** **Blue/green:** two full stacks; switch traffic instantly—fast rollback, **2× capacity/cost** during cutover. **Rolling:** replace pods/instances in waves—no double fleet, but **mixed versions** during window; need backward-compatible APIs. **Canary:** small % traffic to new version, metric-gated promotion—best blast-radius control, needs **traffic splitting** and error budget ([Chapter 24](../24-reliability-engineering/interview-questions.md)). Payments: favor canary + automatic rollback on 5xx/latency SLO breach.

---

## 5. Where should secrets live in CI/CD, and what must never be in the image?

**Answer:** Secrets in **vault/CI secret store** (GitHub Actions secrets, Vault, cloud SM)—injected at runtime via orchestrator or short-lived OIDC to cloud. Never bake **DB passwords, API keys, TLS private keys** into layers (they persist in registries). Use **`.dockerignore`** for `.env`. Pattern: **12-factor** config via env at run; rotate without rebuild. Failure: secret in Git history → revoke, scrub, rebuild pipeline.

---

## 6. What is an artifact repository’s role vs a container registry?

**Answer:** **Artifact repo** (Nexus, Artifactory) stores **JARs, npm, helm charts**—dependency resolution with immutability and license policy. **Container registry** (ECR, GCR, Harbor) stores **OCI images** for runtime. CI publishes both; deploy pulls image by digest. Trade-off: monorepo may publish 20 images per commit—tag and retention policy prevent **unbounded storage cost** (~$0.10/GB-month adds up).

---

## Stretch

## 7. A Docker build that took 3 minutes now takes 25 minutes. What changed?

**Answer:** Check **cache bust**—`COPY .` before dependency install invalidates layers every commit; reorder to `COPY pom.xml` + `mvn dependency:go-offline` first. **No BuildKit cache**, slow network to base image, **apt update** every layer, or scanning step on huge context. **Context size** (.git, `target/` not ignored). Fix: buildkit cache mounts, smaller base, parallel multi-stage. Measure layer history with `docker history`.

---

## 8. How do you implement “build once, deploy to dev/stage/prod” with different config?

**Answer:** Same **image digest** everywhere; **environment-specific config** via env vars, mounted secrets, or external config service—never separate image builds per env unless feature flags baked in (prefer runtime flags). Helm/Kustomize overlays change **replicas, endpoints, log level** only. Validation: prod deploy job references **exact sha** tested in staging; SBOM attached for audit.

---

## 9. Your pipeline is green but production breaks after deploy. What CI gaps do you suspect?

**Answer:** **No integration tests** against real dependencies; **contract tests** missing between services; **config-only** change not tested; **migration** ran in CI sqlite but prod Postgres differs; **load/chaos** not in pipeline; **feature flag** off in test. Add smoke test post-deploy, **synthetic canary** transaction, and compare **golden metrics** 15 min window. Blameless: improve pipeline, not “who clicked deploy.”

---

## 10. When would you choose GitOps (Argo CD/Flux) over push-based CI deploy?

**Answer:** **GitOps:** cluster state declared in Git; controller reconciles—auditable drift detection, easy rollback (`git revert`). **Push deploy:** CI kubectl/helm apply—simpler for small teams, risk of **untacked manual kubectl**. Prefer GitOps at **10+ services** and regulated environments. Trade-off: secret management in Git (sealed secrets/external secrets operator). Links forward to orchestration ([Chapter 14](../14-kubernetes-and-container-orchestration/interview-questions.md)).
