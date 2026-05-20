// Scenario: outbound call to a card processor with bounded wait and connection reuse
// Demonstrates: Transport MaxConnsPerHost, DialContext, context deadline on request
// Trade-off: explicit Transport tuning vs defaults that hide 30s stalls

package main

import (
	"context"
	"net"
	"net/http"
	"time"
)

func NewProcessorClient() *http.Client {
	transport := &http.Transport{
		MaxConnsPerHost:     100,
		MaxIdleConnsPerHost: 20,
		IdleConnTimeout:     90 * time.Second,
		DialContext: (&net.Dialer{
			Timeout: 150 * time.Millisecond,
		}).DialContext,
		ResponseHeaderTimeout: 400 * time.Millisecond,
	}
	return &http.Client{Transport: transport}
}

func Authorize(ctx context.Context, client *http.Client, url, body string) (*http.Response, error) {
	ctx, cancel := context.WithTimeout(ctx, 450*time.Millisecond)
	defer cancel()
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, url, nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("Idempotency-Key", "generated-at-call-site")
	return client.Do(req)
}
