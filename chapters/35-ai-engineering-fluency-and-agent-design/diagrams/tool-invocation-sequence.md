# Tool Invocation — Happy Path, Timeout, and Idempotent Retry

**Supports decision:** How to treat model-proposed tool calls like untrusted RPC: validate, bound time, dedupe replays, and surface partial failure to the user or next step.

```mermaid
sequenceDiagram
  participant O as Orchestrator
  participant L as LLM
  participant V as Schema Validator
  participant T as Tool Adapter
  participant P as External Provider

  O->>L: messages plus tool schemas
  L-->>O: assistant message with tool_calls JSON
  O->>V: validate args against JSON Schema
  alt invalid schema or policy deny
    V-->>O: reject with structured error
    O->>L: tool_result error user_visible
  else valid
    V-->>O: normalized args plus idempotency_key
    O->>T: execute with deadline context
    T->>P: HTTP or SDK call
    alt success within deadline
      P-->>T: 200 body
      T-->>O: tool_result success
    else timeout or ambiguous 5xx
      P-->>T: timeout
      T-->>O: retryable marker
      O->>T: replay with same idempotency_key
      alt second attempt confirms outcome
        P-->>T: 200 or 409 duplicate
        T-->>O: single logical outcome
      else still unknown
        T-->>O: escalate human or compensating step
      end
    end
    O->>L: append tool_result messages
  end
```

**Caption:** Treat **timeouts** as first-class: the model must not assume success; the orchestrator records **idempotency keys** per tool invocation so replays and duplicate LLM proposals do not double-charge or double-email.
