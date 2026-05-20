# Interview Questions: RAG and Retrieval Architecture

**Bank size:** 10  
**Rationale:** RAG is retrieval-first system design; ten questions cover chunking, freshness, hybrid search, and eval—natural bridge from Ch.32 without duplicating full vector DB vendor matrices.  
**Last updated:** 2026-05-20

---

## Core

## 1. In one paragraph, what problem does **RAG** solve that **fine-tuning alone** often does not?

**Answer:** RAG grounds generation in **fresh, citeable** facts (policies, tickets, docs) that change **weekly**—fine-tuning lags and **blurs** version boundaries. It reduces **hallucination** on factual Q&A when retrieval is good and **citations** are enforced. Trade-off: **retrieval quality** becomes the product—bad chunks cause **confident wrong** answers worse than “I don’t know.”

---

## 2. How do you choose **chunk size** and **overlap** for internal technical docs—not textbook defaults?

**Answer:** Size chunks to match **semantic units** (section, procedure) while fitting **embedding model** context (often **256–512 tokens**). Too large: **noise** dilutes embeddings, **latency** rises. Too small: **missing context** (signature lines separate from body). **Overlap** (10–20%) reduces **boundary cuts** mid-step. Validate with **retrieval@k** on labeled questions from **support** tickets—grid search beats guessing.

---

## 3. What is a **freshness architecture** when source documents update continuously?

**Answer:** **Ingest pipeline** on **webhooks** or **CDC** from CMS/wiki; **version** chunks with **updated_at**; **invalidate** or **re-embed** diffs rather than full corpus when possible. Serving: **filter** by `doc_version <= latest_approved` for regulated content. **Staleness SLO**: e.g., **95%** of policy changes searchable within **15 minutes**. Failure: nightly batch jobs → **CEO** reads outdated **compliance** text.

---

## 4. Compare **vector search** vs **keyword (BM25)** for an engineering handbook—when is hybrid better?

**Answer:** Vectors excel at **paraphrase** and **concept** similarity (“timeout retries” vs “backoff”). BM25 excels at **exact** tokens (error codes, **SKU**, **CVE IDs**). Hybrid **reciprocal rank fusion** wins on **mixed** queries common in support. Pure vector struggles on **rare strings** not well represented in embedding space. Measure **MRR** per query type.

---

## 5. What **metadata** do you store beside embeddings—and how does it affect **security**?

**Answer:** Store **source URI**, **ACL/tenant_id**, **classification** (PII/public), **section anchors**, **hash** of content for **change detection**. **Security**: enforce **ACL filter** at query time—**never** rely on the LLM to “ignore” forbidden docs if they reached the prompt. **Leak** pattern: shared index with **missing** tenant filter → cross-customer retrieval.

---

## Stretch

## 6. Name **three retrieval metrics** you’d track in production beyond “embedding cosine similarity.”

**Answer:** **Recall@k / MRR** on labeled sets, **click-through** on citations, **answer unsupported rate** (judge or heuristic checks for **missing** citation spans), **latency** **p95** for retrieval+rerank, **null-result** rate. Operational: **index lag** minutes, **failed ingest** ratio.

---

## 7. **“Contaminated context”**: support tickets include customer PII in retrieved chunks. Mitigate.

**Answer:** **Redact** at ingest (NER + rules), **separate** indexes for **internal** vs **customer-provided** text, **block** certain fields from embedding. **Runtime**: strip patterns before **prompt assembly**. **Compliance**: **regional** indexes, **TTL** on ticket text. Trade-off: aggressive redaction may **remove** clues the model needs—balance with **role-based** retrieval.

---

## 8. Scale estimate: **5M** chunks, **768-dim** float32 vectors—what breaks first in a naive single-index deployment?

**Answer:** **Memory** footprint ~ **5M × 768 × 4 bytes ≈ 15 GB** vectors alone + graph overhead—fits large node but **query QPS** with **HNSW** may bottleneck on **CPU** and **single-shard** latency. **Rebuild** time and **mutation** rate strain **incremental** indexing. Mitigation: **sharding** by tenant/domain, **PQ** compression (accuracy trade), **caching** top queries. Order-of-magnitude: **p95** retrieval budget often **50–200 ms** before rerank.

---

## 9. How do you evaluate **rerankers** (cross-encoder) vs **bi-encoder** retrieval only—latency vs quality?

**Answer:** **Bi-encoder** is fast (ANN); **cross-encoder** rerank on **top 50–200** candidates improves **precision** at cost of **50–300 ms** extra. A/B test on **task success**; if rerank latency blows **SLO**, use **smaller** reranker or **async** two-phase answer (“loading details…”). **Cost**: rerankers multiply **GPU** inference—watch **$/query**.

---

## 10. Design drill: **internal docs assistant** for 50k Confluence pages, **SOC2** tenant isolation. High-level components?

**Answer:** **Crawler** with **OAuth** scoped per workspace, **ACL snapshot** stored per chunk, **embedding worker** idempotent by **content hash**, **vector store** with **partition per tenant**, **query API** that **mandates** `tenant_id` filter, **LLM** service with **citation** post-processor. **DR**: replicate index **region-local** for **data residency**. Cross-link: **security** ([Chapter 25](../25-security-architecture/interview-questions.md)), **LLM production** ([Chapter 32](../32-ai-and-llm-systems-in-production/interview-questions.md)).

---
