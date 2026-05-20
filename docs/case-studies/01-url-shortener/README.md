---
title: Case Study 01 — URL Shortener
description: Design a high-volume URL shortening service with fast redirects, analytics, and abuse protection.
---

# URL Shortener

A URL shortener must map long URLs to compact aliases, route redirects with low latency, and support analytics while controlling abuse. The design should balance simple lookup performance, storage consistency, and scale for hot links and bursty traffic.

## Problem framing

- **Users:** web clients, email campaigns, social media apps, internal tools
- **Peak load:** ~100k QPS redirect traffic, 10k QPS create/update operations
- **Critical path:** resolve alias to target URL and redirect in <50ms
- **Business goals:** high availability, low redirect latency, reliable analytics, abuse mitigation

## Requirements

- Generate and serve short URLs for arbitrary long links
- Redirect users to the original URL with minimal latency
- Track click metrics and top referrers
- Prevent brute-force guessing and large-scale abuse
- Support alias metadata, expiration, and optional custom slugs

## Key constraints

- Redirect latency dominates perceived experience
- Analytics can be eventually consistent and batched
- Hot URLs may receive orders of magnitude more traffic than average
- Custom aliases require uniqueness and collision-safe creation
- Write path must avoid user-facing failures on creation spikes

## Architecture overview

1. **API tier** receives create/update requests and validates payloads.
2. **Alias generator** chooses a short ID using base62 / hash truncation with collision resolution.
3. **Primary store** persists alias → target URL, metadata, creation time, expiration.
4. **Cache layer** serves redirect lookups for hot aliases.
5. **Analytics pipeline** records click events asynchronously to avoid adding latency to redirects.
6. **Abuse protection** enforces rate limits and anti-scraping checks at the edge.

## API sketch

| Method | Path / Event | Notes |
|--------|--------------|-------|
| POST | /shorten | Create a new short URL; supports custom slug and expiry |
| GET | /{alias} | Redirect to target URL |
| GET | /analytics/{alias} | Return aggregate click metrics |
| PUT | /{alias} | Update metadata or TTL (optional) |
| DELETE | /{alias} | Disable a short URL |

## Data model

- `AliasRecord`
  - `alias` (string) — short path
  - `targetUrl` (string)
  - `ownerId` (string)
  - `createdAt` (timestamp)
  - `expiresAt` (timestamp)
  - `customSlug` (bool)
  - `status` (active/disabled)
  - `metadata` (title, tags, campaign)

- `ClickEvent`
  - `alias`
  - `timestamp`
  - `sourceIp`
  - `userAgent`
  - `referrer`
  - `region`

- `AliasStats`
  - `alias`
  - `clicksLastHour`
  - `clicksLastDay`
  - `uniqueUsers`
  - `topReferrers`

## Diagrams

- [Context diagram](./diagrams/context.md)
- [Components diagram](./diagrams/components.md)
- [Core flow diagram](./diagrams/core-flow.md)

## Reliability and failure modes

- **Cache miss on redirect:** fall back to primary store and rebuild cache asynchronously
- **Write collision for alias generation:** use compare-and-set or unique index retry
- **Custom slug conflict:** reject duplicate slugs with a clear client error
- **Secondary store outage:** continue serving cached redirects for hot aliases, degrade analytics
- **Abuse / enumeration:** ratelimit create requests per IP and require CAPTCHA for suspicious flows
- **Stale redirects:** enforce expiration in both cache and primary store, prune expired alias entries

## Analytics pipeline

- Emit click events from the redirect service to a message queue
- Batch process into aggregate counters, time-series store, and dashboard exports
- Use sampling or rollups for high-volume alias traffic to keep cost manageable
- Keep raw event retention separate from aggregate retention

## If I had two more weeks

- Add a self-service dashboard for alias owners with A/B link testing
- Build a multi-tenant tier with separate domains and per-customer quota controls
- Implement advanced abuse detection with anomaly scoring and bot fingerprinting
- Support geo-aware redirect routing or regional edge serving for ultra-low latency

## Three scale triggers

1. **Hot links become viral** → introduce a write-through cache and dynamic hot-key sharding to avoid origin overload
2. **Analytics volume surges** → move click event ingestion to a separate streaming pipeline and add sampling/rollup tiers
3. **Custom alias abuse rises** → enforce stronger validation, CAPTCHA, and per-account create quotas

## Interview prompts

- Why choose cache + primary store over a single database lookup for redirects?
- How would you prevent attackers from enumerating valid aliases?
- What are the trade-offs between fixed-length short IDs and user-provided slugs?
- How does the architecture change if you need sub-10ms redirect latency globally?
