// Scenario: per-user API rate limit — max 100 requests per 60-second sliding window
// Demonstrates: slice queue of timestamps with prune from front
// Trade-off: Redis ZSET sliding window for distributed; local deque for edge gateway

package main

type SlidingWindowRateLimit struct {
	max       int
	windowMs  int64
	timestamps []int64
}

func NewSlidingWindowRateLimit(max int, windowMs int64) *SlidingWindowRateLimit {
	return &SlidingWindowRateLimit{max: max, windowMs: windowMs}
}

func (r *SlidingWindowRateLimit) Allow(nowMs int64) bool {
	cutoff := nowMs - r.windowMs
	i := 0
	for i < len(r.timestamps) && r.timestamps[i] < cutoff {
		i++
	}
	r.timestamps = r.timestamps[i:]
	if len(r.timestamps) >= r.max {
		return false
	}
	r.timestamps = append(r.timestamps, nowMs)
	return true
}
