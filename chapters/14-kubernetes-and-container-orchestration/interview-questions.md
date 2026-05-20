# Interview Questions: Kubernetes and Container Orchestration

**Bank size:** 10  
**Rationale:** Runtime/orchestration chapter; rubric 10 for scheduling, probes, and ops drill.  
**Last updated:** 2026-05-20

---

## Core

## 1. In one paragraph, what problem does Kubernetes solve that Docker alone does not?

**Answer:** Docker packages and runs a process; Kubernetes **schedules many containers across nodes**, restarts failed ones, rolls out versions, exposes stable networking (Service), scales replicas, and mounts config/secrets—handling **desired state vs actual state** continuously. Pain at scale: manual load balancer registration and SSH deploys break at ~20+ services. K8s trades **operational complexity** for uniform APIs for deploy, health, and autoscaling.

---

## 2. What is the difference between a Deployment, a Pod, and a Service?

**Answer:** **Pod:** one or more co-located containers sharing network/IP—ephemeral. **Deployment:** manages ReplicaSet, declares **image, replicas, rollout strategy**—you change Deployments, not individual pods. **Service:** stable **ClusterIP/DNS/load balancer** targeting pod labels—clients don’t track pod IPs. Incident pattern: Deployment healthy but **no Service** or wrong selector → 503 from ingress despite running pods.

---

## 3. Compare liveness, readiness, and startup probes. What happens if you confuse them?

**Answer:** **Readiness:** pod receives traffic only when ready (DB migrations, warmup)—fails → removed from Service endpoints. **Liveness:** fails → kubelet **restarts** container (use for deadlocks, not slow deps). **Startup:** disables other probes during long boot. Mistake: liveness on **downstream DB** → restart loop during outage, amplifying blast radius. Readiness should gate **traffic**; liveness should mean “process is wedged.”

---

## 4. How does the Horizontal Pod Autoscaler (HPA) decide to scale, and what can go wrong?

**Answer:** HPA adjusts Deployment replicas from **metrics** (CPU/memory custom metrics like queue depth). Needs **requests** set on pods for CPU-based scaling; without requests, scheduling is blind. Failure modes: **lag**—scale-up after traffic spike; **flapping**—too low stabilization window; **can't scale**—cluster autoscaler max nodes, **quota**, or image pull slow. Pair HPA with **PDB** (min available) so rollouts don’t violate availability during node drain.

---

## 5. ConfigMap vs Secret—how should apps consume them in production?

**Answer:** **ConfigMap:** non-sensitive config (feature flags, `application.yaml`). **Secret:** credentials, TLS keys—base64 in etcd is **not encryption at rest** unless enabled; use **external secrets operator** or cloud SM integration. Mount as files or env; **env for secrets** can leak in crash dumps/process listings—prefer file mount with 0400. Rotate by **versioned secret name** + rolling restart; avoid hot-reload without app support.

---

## 6. What do `requests` and `limits` on CPU/memory accomplish?

**Answer:** **Requests:** scheduler places pod on node with guaranteed minimum—prevents **overcommit chaos**. **Limits:** cap usage; memory over limit → **OOMKill**; CPU throttled (CFS quota). Under-request → noisy neighbor; over-request → **wasted capacity**. Start from p95 usage + headroom; tune from **actual utilization metrics**. Java services: account for heap + metaspace; don’t set memory limit = heap only.

---

## Stretch

## 7. A rolling update stalls at “0 of 3 updated.” What do you check?

**Answer:** `kubectl describe rs/pod` for **ImagePullBackOff**, **CrashLoopBackOff**, **readiness never true**, **resource insufficient**, **PodSecurity/admission** denial, or **maxUnavailable/maxSurge** math leaving no schedulable pods. Check **events**, previous logs, and whether new version **requires CRD/migration** before old pods terminate. Rollback: `kubectl rollout undo`. Document **pre-stop hook** + graceful shutdown ([Chapter 15](../15-load-balancing-and-traffic-management/interview-questions.md)).

---

## 8. When would you use a StatefulSet instead of a Deployment?

**Answer:** **StatefulSet:** stable network identity (`pod-0`, `pod-1`), ordered rollout, **persistent volumes per replica**—Kafka, ZooKeeper, etcd, stateful caches. **Deployment:** stateless HTTP workers. Mistake: StatefulSet for stateless API because “we might need disk”—adds complexity. For databases, often prefer **managed RDS** over running Postgres in StatefulSet unless strong reason.

---

## 9. Design namespace and network boundaries for 30 microservices in one cluster.

**Answer:** **Namespaces** per team or env (never prod+dev same NS); **NetworkPolicy** default-deny egress/ingress, allowlist per service; **ingress** per public API; internal **mTLS** via mesh optional. Limit **cluster-admin** RBAC; CI uses **namespace-scoped** deploy role. Blast radius: one compromised pod shouldn’t reach **admin etcd** or every DB—segment by label. Cost: policy debugging harder—invest in observability ([Chapter 26](../26-observability/README.md)).

---

## 10. How do you run a one-off database migration safely on Kubernetes?

**Answer:** **Job** with same image as app or dedicated migrator, **backoffLimit** low, **ttlSecondsAfterFinished**, run **before** new app version serves traffic (init container or pipeline step + readiness gate). Use **lease/advisory lock** in DB so only one migrator runs. Never `kubectl exec` manual migrate in prod without audit. Rollback plan: backward-compatible migrations ([Chapter 13](../13-docker-and-cicd/interview-questions.md) pipeline); breaking DDL requires two-phase deploy.
