// Scenario: Order service calls Inventory with remaining context deadline
// Demonstrates: child timeout = min(local cap, parent deadline minus margin)
// Trade-off: context propagation vs Java HttpRequest.timeout — identical budget rule

package main

import (
	"context"
	"net/http"
	"time"
)

func reserve(ctx context.Context, client *http.Client, baseURL, sku string, qty int) (bool, error) {
	deadline, ok := ctx.Deadline()
	if !ok {
		deadline = time.Now().Add(800 * time.Millisecond)
	}
	callBudget := time.Until(deadline) - 50*time.Millisecond
	if callBudget <= 0 {
		return false, context.DeadlineExceeded
	}
	reqCtx, cancel := context.WithTimeout(ctx, callBudget)
	defer cancel()

	req, err := http.NewRequestWithContext(reqCtx, http.MethodPost, baseURL+"/reserve?sku="+sku, nil)
	if err != nil {
		return false, err
	}
	resp, err := client.Do(req)
	if err != nil {
		return false, err
	}
	defer resp.Body.Close()
	return resp.StatusCode == http.StatusOK, nil
}
