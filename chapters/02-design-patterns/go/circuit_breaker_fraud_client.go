// Scenario: optional fraud score enrichment on checkout
// Demonstrates: Circuit breaker sketch — production use gobreaker/resilience4j
// Trade-off: library handles half-open; here shows state + fail-fast intent

package main

import (
	"errors"
	"sync/atomic"
)

type FraudAPI interface {
	FetchScore(orderID string) (int, error)
}

type CircuitBreakerFraudClient struct {
	api       FraudAPI
	failures  atomic.Int32
	threshold int32
	open      atomic.Bool
}

func (c *CircuitBreakerFraudClient) Score(orderID string) (int, bool) {
	if c.open.Load() {
		return 0, false
	}
	score, err := c.api.FetchScore(orderID)
	if err != nil {
		if c.failures.Add(1) >= c.threshold {
			c.open.Store(true)
		}
		return 0, false
	}
	c.failures.Store(0)
	return score, true
}

var ErrFraudUnavailable = errors.New("fraud unavailable")
