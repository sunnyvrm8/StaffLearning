// Scenario: Checkout calls payment provider; transient errors need bounded retries
// Demonstrates: exponential cap + full jitter; same idempotency-key each attempt
// Trade-off: explicit error return vs Java exceptions — same retry policy

package main

import (
	"context"
	"math/rand"
	"net/http"
	"strings"
	"time"
)

func charge(ctx context.Context, client *http.Client, baseURL, idempotencyKey, orderID string) (bool, error) {
	deadline, ok := ctx.Deadline()
	if !ok {
		deadline = time.Now().Add(3 * time.Second)
	}
	for attempt := 0; attempt < 4 && time.Now().Before(deadline); attempt++ {
		reqCtx, cancel := context.WithTimeout(ctx, 400*time.Millisecond)
		req, err := http.NewRequestWithContext(reqCtx, http.MethodPost, baseURL+"/charge",
			strings.NewReader(`{"orderId":"`+orderID+`"}`))
		if err != nil {
			cancel()
			return false, err
		}
		req.Header.Set("Idempotency-Key", idempotencyKey)
		resp, err := client.Do(req)
		cancel()
		if err == nil {
			resp.Body.Close()
			if resp.StatusCode == http.StatusOK {
				return true, nil
			}
			if resp.StatusCode < 500 {
				return false, nil
			}
		}
		jitterSleep(attempt)
	}
	return false, context.DeadlineExceeded
}

func jitterSleep(attempt int) {
	cap := min(2000, 100<<attempt)
	time.Sleep(time.Duration(rand.Int63n(int64(cap))) * time.Millisecond)
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
