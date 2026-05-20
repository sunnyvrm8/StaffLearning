# Interview Questions: Security Architecture

**Bank size:** 10  
**Rationale:** Security spans many subdomains; ten items give a high-yield mock loop without duplicating a full 50-bank OWASP encyclopedia.  
**Last updated:** 2026-05-20

---

## Core

## 1. Name the production stakeholder pains that make “security architecture” a first-class concern—not a late gate.

**Answer:** Breaches and fraud create **legal/revenue** pain; secrets sprawl creates **operability** pain (rotations, outages); compliance (PCI, SOC2) creates **release** pain if discovered late. Architecture answers **where trust boundaries are**, how **identity propagates**, and what **must never cross** a boundary (PAN, PII, signing keys). Weak security shows up as **incident cost** and **velocity collapse** when every deploy needs emergency review.

---

## 2. Summarize OWASP-style risks for a typical B2B SaaS API (no acronym dump—prioritize what breaks revenue).

**Answer:** **Broken access control** (IDOR, tenant bleed) lets one customer read another’s invoices—immediate churn risk. **Injection** (SQL, command) and **deserialization** bugs become RCE. **Auth/session flaws** (weak OAuth, JWT without rotation, stolen refresh tokens) cause account takeover. **SSRF** from webhooks turns your cloud metadata into an attacker’s API. **Supply-chain** compromise ships malware via dependencies or CI tokens. Order-of-magnitude: a single IDOR can expose **100%** of a tenant’s data if keys are sequential UUIDs without authz checks.

---

## 3. Where do you put **mTLS** in a microservices mesh vs “TLS at the edge only,” and what do you trade?

**Answer:** **Edge TLS** terminates at gateway/LB—simple, fewer certs, but **lateral movement** inside the VPC is easier if an attacker lands on a pod. **mTLS service-to-service** (mesh or sidecars) raises **operational** cost (cert rotation, SPIFFE/SPIRE or mesh CA) and **latency** (usually small at ~0.1–0.5 ms per hop in-region) but contains blast radius: compromised frontend cannot impersonate payment service without a valid client cert. Many teams start **edge + network policies**, add **mTLS** when PCI/regulators or zero-trust mandates require **identity per workload**.

---

## 4. How do you manage **secrets** so rotations do not cause Friday-night outages?

**Answer:** Use a **vault/KMS** with **short-lived credentials** (dynamic DB creds, IAM roles for pods) instead of long-lived `.env` files in images. **Inject at runtime** (CSI driver, env from secret store), never bake into layers. **Rotation playbook**: dual-read period (old+new JWT signing keys), **circuit-break** consumers that cache stale secrets, **pre-deploy** canary that validates new secret version. Failure mode: app reads secret once at boot—rotation **silently** breaks new instances until restart storm.

---

## 5. What is **threat modeling** in one concrete artifact, and who owns it?

**Answer:** A **data-flow diagram** (trust boundaries: browser, API, queue, DB, third-party) plus **STRIDE-style** questions: “What if webhook URL is attacker-controlled?” Output: **risk-ranked** mitigations tied to tickets/ADRs, not a slide deck. **Ownership**: service team with **security architect** review at major boundaries (new payment integration, multi-tenant isolation change). Re-run when **data classification** or **network topology** changes—not annually as checkbox.

---

## Stretch

## 6. Compare **session cookies** vs **opaque bearer tokens** vs **signed JWTs** for a first-party web app calling your API—security and ops.

**Answer:** **HttpOnly secure cookies** with **server-side session** give easy **revocation** and smaller theft window if combined with CSRF protections—best for classic web. **Opaque bearer** in mobile: store in **Keychain/Keystore**, rotate refresh tokens, pair with **device binding** if threat model requires. **JWTs**: stateless verification is fast, but **revocation** is hard unless you maintain a **denylist** or short TTL + refresh—stolen JWT is valid until expiry (often 15–60 min). Trade-off: JWT reduces DB hits; sessions reduce stolen-token damage. For **admin** actions, prefer **step-up** or **short-lived** JWT + server checks.

---

## 7. An engineer pasted a **service account key** into GitHub and pushed. What is your incident sequence beyond “rotate the key”?

**Answer:** Assume **exfiltration**: revoke key immediately, **audit cloud API logs** for foreign IPs/unusual regions, **scope blast** (what that SA could read/write). **Forensics**: secret scanning alerts, commit history, fork mirrors. **Fix systemic**: pre-commit secret scan + **branch protection**, **OIDC federation** from CI to cloud (no long-lived keys), **break-glass** keys in vault with **justification**. Communicate to **customers** if data access was possible per policy. Order-of-magnitude: scanners can find keys in **seconds** after push—treat as **P0**.

---

## 8. How do you reason about **supply chain** security for a Java/Go service built in CI?

**Answer:** Lock dependencies (**go.sum**, Maven lockfile), **SBOM** on release artifact, **sign images** (cosign), verify in deploy. CI uses **OIDC** to cloud, not static keys. **Pin base images** and scan for CVEs; accept **risk-based** waivers with expiry. Trade-off: **Dependabot noise** vs real exploits—triage on **reachable** vulns (call graph tools). Failure: compromised build plugin **exfiltrates** env secrets—mitigate with **minimal** CI secrets and **hermetic** builds where possible.

---

## 9. **Compliance** (SOC2, ISO) asks for encryption at rest and access reviews. Where can “checkbox compliance” diverge from **real** security?

**Answer:** At-rest encryption with **everyone-has-KMS-decrypt** in the same account is **tick-box** if IAM is wide. Real security is **least privilege**, **break-glass** audited, **tenant isolation** tests, and **logging** of admin reads. Access reviews catch **stale humans**; they miss **over-privileged service accounts** unless IAM analytics exists. Architect takeaway: map controls to **actual attack paths** (lateral movement, data export APIs), not control library IDs.

---

## 10. Design review: a new **internal admin API** can read any customer’s data for support. List three architectural controls you require before launch.

**Answer:** (1) **Strong authentication** (SSO + hardware key for staff) and **per-request authorization** with **justification ticket ID** logged immutably. (2) **Rate limits + anomaly detection** on bulk export paths; **row-level** filters default to **deny-all** unless ticket context present. (3) **Audit stream** to SIEM with **retention** matching legal hold; optional **session recording** for Tier-1 support. Reject “VPN = trust”—assume **compromised laptop**. Cross-link: API contracts and errors ([Chapter 9](../09-api-design/README.md)), zero-trust networking ([Chapter 8](../08-networking-and-http/README.md)).

---
