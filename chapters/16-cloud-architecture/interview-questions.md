# Interview Questions: Cloud Architecture (AWS-first, portable patterns)

**Bank size:** 10  
**Rationale:** Hosting/foundation chapter before deep distributed theory; rubric 10 for IAM, VPC, and managed-service trade-offs.  
**Last updated:** 2026-05-20

---

## Core

## 1. Why partition workloads across multiple Availability Zones (AZs)?

**Answer:** An **AZ** is an isolated data center (power, networking) within a region—failure of one AZ shouldn’t take the whole service. Pattern: **multi-AZ** for compute (ASG/K8s across AZs), **RDS Multi-AZ** or Aurora for failover, **ELB** cross-AZ. Pain solved: single-datacenter fire/flood/network partition. Trade-off: **cross-AZ data transfer** costs (~$0.01/GB) and **sync replication latency** (~1–2 ms)—still cheaper than regional outage. Multi-AZ ≠ multi-region DR ([Chapter 24](../24-reliability-engineering/interview-questions.md)).

---

## 2. Explain the shared responsibility model in one sentence for a SaaS on AWS.

**Answer:** AWS secures **the cloud** (hypervisor, physical, managed control planes); you secure **in the cloud** (IAM, data encryption, security groups, patching guest OS, app vulnerabilities). You cannot outsource **authz bugs** or public S3 buckets—compliance audits focus on your layer. Use **well-architected** reviews to checklist gaps.

---

## 3. When do you choose RDS/Aurora vs self-managed Postgres on EC2?

**Answer:** **Managed (RDS/Aurora):** automated backups, patching, Multi-AZ failover, ops at **100+ GB** without dedicated DBA time—pay ~20–40% premium. **Self-managed EC2:** exotic extensions, kernel tuning, license constraints, or cost at **very large** steady state with in-house expertise. Architect: default managed until a **named** requirement blocks it; measure **RTO/RPO** and restore drills, not brochure SLAs.

---

## 4. What belongs in a VPC design for a three-tier web application?

**Answer:** **Public subnets:** ALB, NAT gateway (no app servers with public IPs). **Private subnets:** app tier, **no direct internet**—egress via NAT or VPC endpoints. **Data subnets:** RDS, stricter SGs—only app SG on 5432. **IAM roles** for instances/pods (no long-lived keys). **NACLs** optional defense-in-depth; **security groups** stateful allowlists primary. Flow logs for incident forensics. Numbers: NAT is **$/hour + per-GB**—use **VPC endpoints** for S3/Dynamo to cut egress.

---

## 5. How does IAM least privilege differ from “one admin role for the team”?

**Answer:** **Least privilege:** task-scoped roles—CI can push ECR but not delete RDS; app runtime role reads one secret prefix. **Shared admin:** fast incident response but **blast radius** of full account compromise and no audit attribution. Production: **OIDC federation** from GitHub to short-lived roles; break-glass admin with MFA and session logging. Regular **access analyzer** and unused permission reports.

---

## 6. What drives cloud egress cost, and how do architects reduce it?

**Answer:** Egress is data leaving AWS to internet or **cross-region/AZ** in some paths—often **surprise bill** after analytics export or cross-region replication. Mitigations: **CloudFront** for static/video, **same-region** consumers, VPC endpoints, compress payloads, **FinOps tagging** ([Chapter 30](../30-cost-architecture-and-finops/README.md)). Order of magnitude: **$0.05–0.12/GB** internet egress vs pennies internal—design data gravity near compute.

---

## Stretch

## 7. Compare “lift-and-shift” vs “cloud-native” rewrite for a legacy Java monolith.

**Answer:** **Lift-and-shift (EC2/ECS):** fastest migration, keeps debt, misses autoscaling/serverless savings—good for **deadline/compliance**. **Cloud-native (managed services, async, SQS/Lambda where fit):** higher upfront cost, better **elasticity and ops cost** at scale. Hybrid: strangle **read-heavy** paths to serverless, keep core OLTP on RDS. Decision: **reversibility**, team skills, and **2-year TCO** model—not ideology.

---

## 8. You need RPO 1 hour and RTO 4 hours for a regional outage. Sketch the AWS pattern.

**Answer:** **Multi-region** warm standby or pilot light: replicate data (**Aurora Global**, S3 CRR, async replication with lag monitoring). **Route 53** health-checked failover or manual runbook. **RPO 1 h:** backup/snapshot frequency + replication lag alarms. **RTO 4 h:** pre-provisioned AMIs/IaC, **runbook** for DNS flip, game days quarterly. Not every service needs active-active—**cost doubles**. Document what is **lost** during lag (last hour of writes).

---

## 9. A startup asks “serverless vs Kubernetes?” for 5 engineers and unpredictable traffic.

**Answer:** **Serverless (Lambda + API GW + Dynamo):** minimal ops, pay per use—cold starts (100 ms–2 s) and **vendor limits** hurt steady high QPS. **EKS:** portable, fits long connections and **K8s ecosystem**—ops tax unless managed. Middle: **Fargate** or **App Runner**. For 5 engineers and spiky MVP: start **managed serverless** on critical path; move hot stable services to containers when **cost/latency** curves cross (~ sustained 1k+ RPS). Portable patterns: **12-factor**, OpenAPI, avoid proprietary lock-in in core domain.

---

## 10. How do you avoid “architecture astronaut” cloud diagrams that don’t match reality?

**Answer:** Tie every box to **SLO, owner team, and data store**; run **game days** and **chaos** on AZ failure; tag resources for **cost per feature**; capture decisions in ADRs with **rejected options** ([Chapter 31](../31-architecture-governance/README.md)). Review: can we **deploy weekly**, **rotate secrets**, and **restore backup** this quarter? If no, simplify—one region multi-AZ beats three regions on paper. Cross-link containers ([Chapter 14](../14-kubernetes-and-container-orchestration/interview-questions.md)) and load balancing ([Chapter 15](../15-load-balancing-and-traffic-management/interview-questions.md)).
