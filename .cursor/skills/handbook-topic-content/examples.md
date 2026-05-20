# Usage Examples

## Example 1 — Full chapter package

**User:** Create handbook content for chapter 10 Caching Strategies.

**Agent actions:**

1. Rubric → **50 questions** (core chapter).
2. Create `chapters/10-caching-strategies/`.
3. README: cache-aside vs read-through vs write-through; stampede; TTL invalidation; pain = DB load + latency on catalog service.
4. Diagrams: `read-path.md`, `invalidation.md`, `stampede.md`.
5. Code: cache-aside (Java + Go), singleflight or lock for stampede (Java + Go).
6. `interview-questions.md` with 50 question-and-answer pairs (no difficulty/type tags).
7. Cross-link ch. 9, 15, 16.

---

## Example 2 — Case study with 25 questions

**User:** Case Study 7 Rate Limiter — diagrams and 25 interview questions.

**Agent actions:**

1. `case-studies/07-rate-limiter/`.
2. README uses case-study addendum (API, napkin math, scale triggers).
3. Diagrams: context, token-bucket sequence, distributed topology.
4. Java + Go: token bucket or sliding window (same scenario).
5. 25 Q&A pairs: ~40% system-design depth, ~30% trade-off focused (per rubric, no tags in file).

---

## Example 3 — Interview bank only

**User:** Add 100 questions for Distributed Systems (chapter 15).

**Agent actions:**

1. Open or create `chapters/15-distributed-systems/interview-questions.md`.
2. Apply 100-bank section structure including Rapid Fire + Scenario Drills.
3. Add link in existing README if present.

---

## Example 4 — Leadership (small bank)

**User:** Content for "Influence Without Authority" from Principal framework.

**Agent actions:**

1. `leadership/influence-without-authority/`.
2. README: STAR-friendly scenarios, no code.
3. Optional decision-flow diagram.
4. **10 Q&A pairs**, mostly behavioral and trade-off themed.

---

## Example 5 — Cheatsheet mode

**User:** Cheatsheet for CAP theorem — quick revision.

**Agent actions:**

1. `chapters/16-cap-theorem/README.md` ≤800 words.
2. One diagram (CP vs AP under partition).
3. **10 questions** only; no code unless user asks.
