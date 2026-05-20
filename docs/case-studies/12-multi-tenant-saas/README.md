---
title: Case Study 12 — Multi-Tenant SaaS
description: Design a multi-tenant SaaS platform with tenant isolation, resource fairness, and per-tenant SLAs.
---

# Multi-Tenant SaaS

A multi-tenant SaaS platform must balance tenant isolation, shared infrastructure efficiency, and fair resource usage. The design should support multiple isolation models and provide strong visibility into per-tenant performance.

## Problem framing

- **Users:** tenant admins, application users, platform operators
- **Peak load:** highly variable per tenant with bursty usage patterns
- **Critical path:** maintain SLA compliance while sharing infrastructure
- **Business goals:** enable rapid onboarding, minimize noisy neighbor impact, support service tiers

## Requirements

- Isolate tenant data and configuration
- Support per-tenant quotas and service levels
- Monitor usage and enforce noisy neighbor protections
- Allow tenant-specific customizations and feature flags
- Provide tenant-aware billing and reporting

## Key constraints

- Strong isolation increases operational cost; shared tenancy is more efficient
- Some tenants require strict compliance or dedicated resources
- Fairness must be enforced without over-allocating capacity
- Multi-tenant data models complicate schema and query planning
- Monitoring must capture both aggregate and tenant-specific health

## Architecture overview

- **Tenant registry** stores configuration, quotas, and feature flags.
- **Routing layer** maps requests to tenant-aware services.
- **Isolation model** may be shared schema, separate schema, or separate clusters.
- **Quota manager** enforces per-tenant limits and throttles.
- **Observability stack** collects tenant-specific metrics and logs.

## API sketch

| Method | Path | Notes |
|--------|------|-------|
| POST | /tenants | Create tenant |
| GET | /tenants/{id}/usage | Tenant billing and usage |
| GET | /resources | Tenant-specific data access |
| POST | /tenant/{id}/quota | Update tenant limits |

## Data model

- `Tenant`
  - `tenantId`
  - `plan`
  - `configuration`
  - `quota`
  - `createdAt`

- `TenantMetric`
  - `tenantId`
  - `cpuUsage`
  - `requestCount`
  - `errorRate`

- `ResourceOwnership`
  - `resourceId`
  - `tenantId`
  - `isolationLevel`

## Diagrams

- [Context diagram](./diagrams/context.md)
- [Components diagram](./diagrams/components.md)
- [Core flow diagram](./diagrams/core-flow.md)

## Reliability and failure modes

- **Noisy neighbor:** enforce caps and isolate heavy tenants to dedicated resources
- **Tenant data leak:** use strict tenant-aware filters and schema separation where required
- **Quota policy drift:** centralize tenant policies and audit change history
- **Onboarding failures:** validate tenant provisioning end-to-end and rollback partial state
- **Custom configuration bugs:** use safe defaults and feature-gated rollout for tenant-specific behavior

## If I had two more weeks

- Add self-service tenant onboarding and sandbox environments
- Add per-tenant health dashboards and anomaly alerts
- Add migration tooling for moving tenants between isolation tiers

## Three scale triggers

1. **Tenant growth variance** → separate high-traffic tenants onto dedicated clusters
2. **Compliance requests** → support dedicated tenancy and stricter audit logs
3. **Platform-wide incidents** → add tenant blast-radius isolation and rollback capabilities

## Interview prompts

- What are the advantages and disadvantages of shared schema vs separate schema tenancy?
- How would you mitigate a noisy neighbor in a multi-tenant SaaS platform?
- How do you design tenant-aware observability without overwhelming the metrics pipeline?
