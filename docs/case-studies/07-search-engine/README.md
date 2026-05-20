---
title: Case Study 07 — Search Engine
description: Design a search engine with indexing, ranking, and freshness for large document collections.
---

# Search Engine

A search engine must ingest documents, build and maintain an index, and answer queries with relevance and low latency. The design should balance indexing throughput, query speed, and freshness for frequently updated content.

## Problem framing

- **Users:** search consumers, analytics tools, internal dashboards
- **Peak load:** ~100k query QPS, 5k document updates per second
- **Critical path:** return relevant results in <200ms
- **Business goals:** query accuracy, index freshness, repeatable ranking

## Requirements

- Index documents and support full-text search with filters
- Serve ranked query results with pagination
- Allow incremental updates and delete operations
- Scale across shards and replicas for query load
- Provide metrics for query latency and index health

## Key constraints

- Freshness and throughput are in tension: frequent writes can slow query performance
- Ranking may depend on scoring signals that change over time
- Shard balancing is required for both query and index load
- Some queries may require expensive joins or faceting
- The index needs to support fault-tolerant replica failover

## Architecture overview

- **Ingestion pipeline** processes documents and emits index events.
- **Index builder** tokenizes and writes inverted index segments.
- **Query service** merges shard results and applies ranking.
- **Metadata store** holds schema, synonyms, and ranking settings.
- **Replica layer** provides low-latency query reads and high availability.

## API sketch

| Method | Path | Notes |
|--------|------|-------|
| POST | /index | Add or update document |
| POST | /delete | Remove document |
| GET | /search | Execute query |
| GET | /health | Check shard/replica status |

## Data model

- `Document`
  - `docId`
  - `content`
  - `fields`
  - `createdAt`
  - `updatedAt`

- `InvertedIndexEntry`
  - `term`
  - `docIds`
  - `postings`
  - `termFrequency`

- `ShardState`
  - `shardId`
  - `status`
  - `replicas`
  - `segmentGeneration`

## Diagrams

- [Context diagram](./diagrams/context.md)
- [Components diagram](./diagrams/components.md)
- [Core flow diagram](./diagrams/core-flow.md)

## Reliability and failure modes

- **Index lag:** use nearline ingestion and merge segments asynchronously
- **Shard hotspot:** re-shard or re-balance based on query and update load
- **Replica failure:** fail over searches to healthy replicas and rebuild lagging copies
- **Stale ranking data:** isolate signals that change frequently (clicks, recency) from the base index
- **Expensive queries:** rate-limit or serve approximate results for heavy queries

## If I had two more weeks

- Add query auto-completion and typo tolerance
- Implement a relevance feedback loop from click signals
- Add per-tenant search profiles and isolated ranking rules

## Three scale triggers

1. **Query volume grows** → add read-only replicas and query fan-in optimization
2. **Index update rate increases** → separate write and merge pipelines with versioned segments
3. **Freshness demand rises** → shorten ingest-to-search latency with streaming index updates

## Interview prompts

- How do you design an index for both read performance and frequent updates?
- What are the trade-offs between shard count and query fan-out?
- How would you handle a hot term that attracts disproportionate query traffic?
