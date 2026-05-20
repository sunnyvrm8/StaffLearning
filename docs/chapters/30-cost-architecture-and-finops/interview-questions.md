# Interview Questions: Cost Architecture and FinOps

**Bank size:** 10  
**Rationale:** FinOps ties money to architecture decisions; ten questions cover unit economics, tagging discipline, and exec-ready narratives without a full cloud pricing encyclopedia.  
**Last updated:** 2026-05-20

---

## Core

## 1. What is **unit economics** in a SaaS product—and why do architects own part of it?

**Answer:** Unit economics maps **revenue per customer** (or per seat, per transaction) to **variable cost** to serve: compute, storage, **egress**, third-party APIs, support load. Architects own **COGS drivers**: chatty cross-AZ traffic, **unbounded** logs, **over-provisioned** always-on GPU, **N+1** queries that force bigger DBs. If **gross margin** drops as you scale, it is often an **architecture** problem, not only “finance should watch the bill.”

---

## 2. Describe a **tagging and allocation** model that survives a real re-org—not ideal tags on a slide.

**Answer:** Enforce **required** tags at deploy time: `service`, `env`, `team`, `cost_center`, `tenant_tier`. **Inherit** tags from **folders/accounts** for shared data stores. **Map** untagged spend to **escrow** bucket billed to **platform** until fixed—creates incentive. **Showback** dashboards per team weekly; **chargeback** only when finance mature. Failure: **80%** “unknown” spend after multi-account merger—**retrofit** scripts and **block** new resources without tags ([Chapter 16](../16-cloud-architecture/interview-questions.md) patterns).

---

## 3. Why does **data egress** surprise bills—and what architectural patterns reduce it?

**Answer:** Clouds charge **egress** cross-region/AZ and to internet; analytics pipelines that **pull** full replicas cross-region explode cost. Patterns: **colocate** compute with data, **CDN** for static, **compress** payloads, **event streaming** with **binary** formats, **avoid** shipping raw logs to third parties without **sampling**. Order-of-magnitude: **$0.05–0.12/GB** egress adds up at **TB/month**—can exceed **compute** for log-heavy systems.

---

## 4. Compare **reserved instances / savings plans** vs **on-demand** scaling for a steady-state data pipeline.

**Answer:** **Commitments** cut **30–50%+** for **predictable** baseline (always-on Kafka brokers, core DB primaries). **On-demand** (or **spot** for fault-tolerant batch) covers **spiky** bursty workers. Risk: **over-commit** when workload **migrates** to managed SaaS—stranded capacity. Architect process: **rightsizing** first (avoid buying bigger waste cheaper), then **commit** the **floor**.

---

## 5. What is **right-sizing** in practice—beyond “use smaller VMs”?

**Answer:** Match **CPU/memory/IO** to measured **p95** utilization with **headroom** policy (e.g., 40% target at peak). Use **vertical** rightsizing plus **autoscaling** policies that **actually** scale down (not min= max). Include **DB storage** growth projections and **IOPS** tier. Pair with **performance** chapter: rightsizing that drops **p99** is **negative** savings if SLO breaks ([Chapter 27](../27-performance-engineering/interview-questions.md)).

---

## Stretch

## 6. **Multi-tenant** product: one enterprise customer stores **100x** data of median tenant. How do you architect **fair cost** and **margin**?

**Answer:** **Metered** pricing on **storage + API calls + egress** with **tiered** rates; **soft quotas** with **upsell**. Engineering: **isolate** heavy tenant to **dedicated** pool to protect **shared** COGS; **archive** cold data to **cheaper** tier (S3 Glacier). Finance alignment: **COGS per tenant** dashboard prevents **surprise** renewals.

---

## 7. Tell a **FinOps culture failure** and the leading metric you’d watch to detect it early.

**Answer:** Teams optimize **feature velocity** with **always-on** preview envs per PR and **full** prod data copies—**cloud bill doubles** in a quarter. Early metric: **non-prod** spend as **%** of total creeping > **30–40%** without **correlated** test value. Fix: **TTL** automation, **synthetic** data, **shared** ephemeral pools.

---

## 8. When is **Spot** acceptable for **production** workloads—and when is it reckless?

**Answer:** Acceptable for **stateless batch** workers with **checkpointing**, **retry**, and **SLO** that tolerates **preemption** (minutes). Reckless for **single-primary** DB or **latency-critical** synchronous API without **fallback pool**. Hybrid: **Spot + on-demand** mixed node groups with **priority** classes—ensure **critical pods** land on stable capacity ([Chapter 14](../14-kubernetes-and-container-orchestration/README.md)).

---

## 9. You must cut **20% cloud cost** in 90 days without freezing features. What **architecture levers** do you propose first?

**Answer:** (1) **Delete** unused volumes/snapshots and **idle** LB rules. (2) **Log/trace** retention and **sampling** tuning. (3) **Cache** hot read paths to **downsize** DB ([Chapter 12](../12-caching-strategies/interview-questions.md)). (4) **Regional** consolidation for **dev** stacks. De-prioritize micro-optimizing **$50/mo** services before **six-figure** data egress or **GPU** idle.

---

## 10. Prepare a **one-slide exec summary** after a bill spike: what three numbers and one decision ask?

**Answer:** Numbers: **$ delta** vs prior month, **top three** services/SKUs driving delta, **customer/revenue impact** (if any). Decision ask: **approve** rightsizing + **TTL** policy with **owner**, or **fund** dedicated FinOps sprint. Avoid deep **SKU** acronyms—translate to **“we paid for X TB egress because analytics moved cross-region.”**

---
