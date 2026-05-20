# AI Recommendation System — Context

**Supports decision:** show offline feature generation and online recommendation serving.

```mermaid
flowchart TB
  event[Event Pipeline]
  feature[Feature Store]
  model[Model Training]
  serve[Recommendation Service]
  user[User]

  event --> feature
  feature --> model
  event --> serve
  serve --> user
```