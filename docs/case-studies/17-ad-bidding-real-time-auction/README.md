---
title: Case Study 17 — Ad Bidding / Real-Time Auction
description: Design a real-time ad auction system with low latency, budget pacing, and fraud protection.
---

# Ad Bidding / Real-Time Auction

A real-time auction system must evaluate bids, enforce budgets, and return winners within tens of milliseconds. The design should protect against fraud, pacing violations, and incomplete auction pipelines while supporting high auction volume.

## Problem framing

- **Users:** advertisers, bidding platforms, publishers, ad servers
- **Peak load:** tens of thousands of auctions per second
- **Critical path:** complete auction in <50ms for ad insertion
- **Business goals:** maximize yield, obey pacing, reduce fraud, ensure reliability

## Requirements

- Accept bid requests and apply targeting rules
- Evaluate bids against budget, floor price, and campaign constraints
- Return winners with creative and tracking metadata
- Support fraud detection, blacklist/whitelist rules, and budget pacing
- Provide auction logs and performance metrics

## Key constraints

- The auction path is extremely latency-sensitive
- Budget and pacing decisions must be accurate across many auctions
- Fraud detection must be fast and may require external signals
- Partial failures can lead to lost revenue or bad ad experiences
- High cardinality of targeting criteria makes bid evaluation expensive

## Architecture overview

- **Bid request service** receives ad requests from publishers.
- **Campaign service** evaluates budgets, targeting, and pacing.
- **Auction engine** scores bids, applies rules, and selects a winner.
- **Creative service** returns ad content and tracking metadata.
- **Logging pipeline** captures auction outcomes for billing and analytics.

## API sketch

| Method | Path | Notes |
|--------|------|-------|
| POST | /bid | Process bid request |
| GET | /campaign/{id} | Query campaign budget |
| GET | /health | Check auction health |

## Data model

- `BidRequest`
  - `requestId`
  - `publisherId`
  - `inventoryAttributes`
  - `timestamp`

- `Campaign`
  - `campaignId`
  - `budget`
  - `pacingRules`
  - `targetingCriteria`
  - `status`

- `AuctionResult`
  - `auctionId`
  - `winnerCampaignId`
  - `winPrice`
  - `timestamp`

## Diagrams

- [Context diagram](./diagrams/context.md)
- [Components diagram](./diagrams/components.md)
- [Core flow diagram](./diagrams/core-flow.md)

## Reliability and failure modes

- **Latency spikes:** prefetch campaign data and use in-memory budget caches
- **Budget overspend:** apply strict accounting and pessimistic reservation on win
- **Fraud / invalid bids:** use guardrails and drop suspicious requests early
- **Auction engine overload:** add tiered bidding and fast path evaluation
- **Partial marketplace failure:** degrade to default ads or safe fallback experiences

## If I had two more weeks

- Add a layered auction with first-price and second-price logic
- Add advertiser pacing simulator and what-if analytics
- Add fraud signal enrichment and scoring for faster early rejection

## Three scale triggers

1. **Auction volume growth** → add precomputed candidate sets and tiered evaluation
2. **Budget mismatches** → improve pacing logic and real-time budget reconciliation
3. **Latency budget compression** → move more checks into in-memory path and cache results

## Interview prompts

- What is budget pacing and why does it matter in ad auctions?
- How do you balance latency and accuracy in bid evaluation?
- How would you design a safe fallback when the auction system is degraded?
