# Interview Questions: Platform Engineering and Internal Developer Platforms

**Bank size:** 10  
**Rationale:** IDP topics span culture and tooling; ten questions hit golden paths, guardrails, and platform–product tension without a full catalog of tools.  
**Last updated:** 2026-05-20

---

## Core

## 1. What problem does an **Internal Developer Platform (IDP)** solve that “a good DevOps team” alone does not?

**Answer:** IDP turns **tribal knowledge** into **self-service products** (deploy, env, data, compliance) with **defaults** that scale to **dozens of teams**—reducing **tickets** and **snowflake clusters**. DevOps alone often becomes **heroic** routing; IDP **productizes** paved roads with **SLAs**, **docs**, and **templates**. Pain without IDP: **weeks** to onboard a service; **inconsistent** security baselines; **merge** conflicts on Helm copied 50 ways.

---

## 2. Define a **golden path** with a concrete example—not a buzzword.

**Answer:** A **golden path** is the **opinionated default** for “create a Java/Go microservice in our org”: repo template → **CI** with tests + SAST → **build/push** image → **deploy** to **staging** with **ingress + mTLS** on → **observability** dashboards auto-wired → **feature flags** stub. Teams **can** leave the path with an **ADR**, but default is **fast and safe**. Example outcome: new service **onboards in hours**, not **two sprints** of YAML archaeology.

---

## 3. How do you balance **self-service** with **guardrails** so you are not “blocking innovation”?

**Answer:** **Policy-as-code** (OPA, admission hooks) blocks **known bad** (public S3, `privileged: true`) while allowing **exceptions** via **reviewed waiver** with **expiry**. **Self-service** for **non-risk** items (preview envs, feature branches); **workflow** for **high-risk** (prod data access). Communicate **why** the guardrail exists with **escape hatch** time—e.g., **24h** break-glass. Measure **time-to-prod** and **policy exceptions/month**—if exceptions explode, the path is wrong.

---

## 4. Compare **“platform team builds everything”** vs **“federated platform + enabling teams.”**

**Answer:** Central everything: consistent UX but **bottleneck** on platform backlog—product teams wait. Federated: **golden libraries** owned by **area** platform squads (data, runtime, security) with **shared** standards—faster local iteration, risk of **drift** without **fitness functions** ([Chapter 31](../31-architecture-governance/interview-questions.md)). Staff answer: pick **federation** when org > **~150 engineers** unless you have **very strong** internal product management for platform.

---

## 5. What **metrics** prove an IDP is working for executives—not vanity DORA charts?

**Answer:** **Lead time** for changes on golden-path services, **change failure rate**, **MTTR** (platform incidents included), **%** of new services on **approved templates**, **mean time to first prod deploy** for a new team, **ticket volume** to platform per deploy. Tie to **revenue protection**: fewer **SEV-1** from misconfig. Avoid only “**N** services onboarded”—count **active** usage and **developer NPS** qualitatively.

---

## Stretch

## 6. Your **service catalog** is stale a week after launch. What systemic fixes beat “please update YAML”?

**Answer:** **Generate** catalog from **live** deploy metadata (Git tags, K8s labels, CI attestations), **enforce** ownership fields in **CI merge gate**, **link** to **on-call** rotation automatically. **Scorecards** (maturity model) surface **missing** SLOs or **EOL** runtimes with **blocking** promotions over time. Stale catalog is a **symptom** of platform not being **in the path** of truth.

---

## 7. **Build vs buy** for CI/CD: when do you insist on **managed** SaaS vs self-hosted runners?

**Answer:** Prefer **managed** when **security isolation** (ephemeral runners), **compliance attestations**, and **maintenance** cost outweigh **egress** or **custom hardware** needs. Self-host when you need **GPU** builders, **air-gapped** environments, or **exotic** dependencies—but then you **own** patching and **SPOF**. Hybrid: **managed orchestration** + **self-hosted** workers behind strict **network** controls.

---

## 8. Incident: **platform outage** blocks all deploys for 2 hours during a product launch. What post-incident architecture moves do you propose?

**Answer:** **Blast radius reduction**: **regional** control planes, **cached** last-known-good deploy artifacts, **manual** rollback path that does **not** depend on platform UI. **SLO** on platform with **error budget** governance—freeze feature work if budget blown. **Game days** for “platform down.” Culturally: **no single throat** for releases—**hermetic** scripts exist locally with **signed** bundles.

---

## 9. Product wants **Kubernetes access** for every engineer “to learn.” What is your stance and alternative?

**Answer:** Raw cluster **admin** is **training for outages**—RBAC **namespace** sandboxes, **GitOps** previews, **disposable sandbox** environments with **fake** data. Teach with **simulators** and **read-only** `kubectl` for prod via **audited** break-glass. Trade-off: friction vs **safety**—default **deny** with **just-in-time** elevation (see [Chapter 25: Security Architecture](../25-security-architecture/interview-questions.md) themes).

---

## 10. As a **Staff** engineer, how do you resolve conflict between **platform standardization** and a team shipping **novel** streaming tech?

**Answer:** Run a **time-boxed pilot** with **explicit** risks (operability, hiring, incident playbooks) and **success metrics** (p99, cost, MTTR). If pilot wins, **promote** to golden path component; if not, **document rejection** in ADR with **sunset** plan for the snowflake. Influence: connect **novelty cost** to **on-call** rotation they already hate—data beats ideology.

---
