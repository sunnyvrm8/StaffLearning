// Scenario: token bucket rate limiting in Go
// Demonstrates: refill logic and allowance check in a distributed rate limiter
// Trade-off: fast local decision versus strict global coordination

package ratelimiter

import (
	"context"
	"math"
	"time"
)

type TokenBucket struct {
	Tokens      float64
	LastUpdated int64
}

type RateStore interface {
	Get(key string) (*TokenBucket, error)
	Save(key string, bucket *TokenBucket) error
}

func Allow(ctx context.Context, store RateStore, key string, capacity int, refillRate float64) (bool, error) {
	now := time.Now().UnixMilli()
	bucket, err := store.Get(key)
	if err != nil {
		return false, err
	}
	if bucket == nil {
		bucket = &TokenBucket{Tokens: float64(capacity), LastUpdated: now}
	}

	elapsed := float64(now-bucket.LastUpdated) / 1000.0
	bucket.Tokens = math.Min(float64(capacity), bucket.Tokens+elapsed*refillRate)
	if bucket.Tokens < 1 {
		return false, store.Save(key, bucket)
	}

	bucket.Tokens -= 1
	bucket.LastUpdated = now
	return true, store.Save(key, bucket)
}
