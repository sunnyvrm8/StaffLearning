// Scenario: checkout aggregates inventory + fraud with a hard user-facing deadline
// Demonstrates: context.WithTimeout + errgroup for fan-out
// Trade-off: explicit error returns vs Java exceptions — map errors at HTTP boundary

package main

import (
	"context"
	"errors"
	"time"

	"golang.org/x/sync/errgroup"
)

type Inventory interface {
	Available(ctx context.Context, sku string) (int, error)
}
type Fraud interface {
	Allow(ctx context.Context, orderID string) (bool, error)
}

func Checkout(ctx context.Context, orderID, sku string, inv Inventory, fraud Fraud) (bool, error) {
	ctx, cancel := context.WithTimeout(ctx, 800*time.Millisecond)
	defer cancel()

	var qty int
	var ok bool
	g, ctx := errgroup.WithContext(ctx)
	g.Go(func() error {
		var err error
		qty, err = inv.Available(ctx, sku)
		return err
	})
	g.Go(func() error {
		var err error
		ok, err = fraud.Allow(ctx, orderID)
		return err
	})
	if err := g.Wait(); err != nil {
		if errors.Is(err, context.DeadlineExceeded) {
			return false, err
		}
		return false, err
	}
	return qty > 0 && ok, nil
}
