---
title: Case Study 02 — Payment System
description: Design an end-to-end payment processing system with idempotency, sagas, and PCI-aware boundaries.
---

# Payment System

A payment system must process purchase intents, interact with external payment providers, and preserve correctness through failures, retries, and refund flows. The architecture should minimize user-facing latency while enforcing strong consistency for money movement.

## Problem framing

- **Users:** shoppers, checkout services, merchant platforms
- **Peak load:** ~5k QPS payment requests, 20k QPS status checks
- **Critical path:** authorize/capture in <200ms for checkout latency
- **Business goals:** no double charge, transparent retries, safe refund and dispute handling

## Requirements

- Accept payment requests and generate unique idempotency keys
- Authorize, capture, and settle across multiple providers
- Support refunds, partial refunds, voids, and chargeback workflows
- Expose transaction status and audit history
- Protect cardholder data and maintain PCI-lite boundaries

## Key constraints

- External provider latency is variable and can exceed 5s
- Idempotency must survive retries and duplicate requests
- Multiple services may update order and payment state concurrently
- Failure compensation must avoid over-capture and inconsistent order status
- Compliance requires encryption, minimal scope, and audit logs

## Architecture overview

- **Checkout API** handles payment submission and status queries.
- **Payment coordinator** implements the saga for authorization, capture, and settlement.
- **State store** persists payment records, charge events, and idempotency metadata.
- **Outbox / event bus** publishes follow-up events to downstream order/notification systems.
- **Provider adapters** isolate different gateway semantics and error mapping.

## API sketch

| Method | Path | Notes |
|--------|------|-------|
| POST | /payments | Create payment request with idempotency-key |
| GET | /payments/{id} | Query payment state |
| POST | /payments/{id}/refund | Request refund |
| POST | /payments/{id}/void | Void authorization before capture |

## Data model

- `PaymentRecord`
  - `paymentId`
  - `orderId`
  - `amount`
  - `currency`
  - `status` (pending, authorized, captured, failed, refunded)
  - `providerId`
  - `externalTransactionId`
  - `createdAt`
  - `lastUpdatedAt`

- `IdempotencyRecord`
  - `idempotencyKey`
  - `paymentId`
  - `requestHash`
  - `response`
  - `expiresAt`

- `RefundEvent`
  - `refundId`
  - `paymentId`
  - `amount`
  - `status`
  - `providerResponse`

## Diagrams

- [Context](./diagrams/context.md)
- [Components](./diagrams/components.md)
- [Core flow](./diagrams/core-flow.md)

## Code examples

- [Java](./java/PaymentCoordinator.java)
- [Go](./go/payment_coordinator.go)

## Reliability and failure modes

- **Duplicate submission:** use idempotency lookup before processing; retry safe if key already exists
- **Provider timeout:** mark payment pending and retry using the same idempotency key
- **Partial failure during capture:** compensate by voiding or issuing a refund depending on state
- **Outbox failure:** retry publishing events; ensure reconciliation between payment state and order notifications
- **Concurrent updates:** serialize state transitions through a single coordinator per payment or use compare-and-set

## Code sketch: idempotent request handling

```java
// Pseudocode for idempotent payment creation
public PaymentResponse handleCreate(PaymentRequest req) {
  var record = idempotencyStore.lookup(req.idempotencyKey);
  if (record != null) { return record.response(); }

  var payment = paymentStore.create(req);
  var response = processPayment(payment);
  idempotencyStore.save(req.idempotencyKey, response);
  return response;
}
```

```go
func handleCreate(req PaymentRequest) (PaymentResponse, error) {
  if rec, err := idempotencyStore.Lookup(req.IdempotencyKey); err == nil && rec != nil {
    return rec.Response, nil
  }
  payment := paymentStore.Create(req)
  resp := processPayment(payment)
  idempotencyStore.Save(req.IdempotencyKey, resp)
  return resp, nil
}
```

## If I had two more weeks

- Add a reconciliation dashboard for provider-delayed settlements
- Implement a self-service refunds portal and dispute workflow
- Add synthetic monitoring for provider failover and payment latency

## Three scale triggers

1. **Payment provider latency spikes** → add async retry + payment status polling instead of blocking checkout
2. **High retry / duplicate requests** → increase idempotency window and use request hash dedupe
3. **Multiple payment methods** → isolate adapters and add circuit breakers per provider

## Interview prompts

- Why is idempotency critical in a payment flow and how do you guarantee it?
- How do you design a saga for authorize/capture/refund without double charging?
- What is the trade-off between synchronous and asynchronous checkout in this system?
