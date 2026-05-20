// Scenario: Checkout API limits concurrent payment calls during flash sale
// Demonstrates: buffered channel as acquire slot — 503 when pool full
// Trade-off: channel vs semaphore — same backpressure semantics in Go

package main

import (
	"context"
	"errors"
	"time"
)

var errOverloaded = errors.New("overloaded")

type BoundedInFlight struct {
	slots chan struct{}
}

func NewBoundedInFlight(max int) *BoundedInFlight {
	return &BoundedInFlight{slots: make(chan struct{}, max)}
}

func (b *BoundedInFlight) Run(ctx context.Context, fn func(context.Context) error) error {
	select {
	case b.slots <- struct{}{}:
		defer func() { <-b.slots }()
		return fn(ctx)
	case <-time.After(50 * time.Millisecond):
		return errOverloaded
	case <-ctx.Done():
		return ctx.Err()
	}
}
