# Context Map — Integration Relationships

**Supports decision:** Whether fulfillment should **conform** to checkout events, **translate** via ACL, or **negotiate** a partnership API.

```mermaid
flowchart LR
  checkout[Checkout]
  fulfillment[Fulfillment]
  billing[Billing]
  analytics[Analytics]
  legacyWMS[Legacy WMS]
  taxProvider[External Tax API]

  checkout -->|Customer-Supplier: OrderPlaced| fulfillment
  checkout -->|Customer-Supplier: InvoiceRequested| billing
  checkout -->|Open Host: ProductCatalog API| analytics
  fulfillment -->|Anti-Corruption Layer| legacyWMS
  billing -->|Conformist| taxProvider
```

| Arrow | Pattern | You document |
|-------|---------|--------------|
| Checkout → Fulfillment | Customer–supplier | Event schema version, idempotency key, DLQ owner |
| Fulfillment → Legacy WMS | ACL | Mapping table, retry policy, vendor outage fallback |
| Checkout → Analytics | Conformist (often) | Analytics accepts checkout event shape; no write-back |

**Conway’s law:** If the map says customer–supplier but **one manager owns both teams**, expect the “supplier” to be ignored—realign teams or integration governance ([Chapter 31: Architecture Governance](../../31-architecture-governance/README.md)).
