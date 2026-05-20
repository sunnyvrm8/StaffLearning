// Scenario: mobile BFF loads order page from Order, User, Shipment services in parallel
// Demonstrates: goroutines + per-call context timeout; degrade shipment on timeout
// Trade-off: manual sync vs errgroup — fewer deps in handbook snippets

package main

import (
	"context"
	"errors"
	"sync"
	"time"
)

type OrderAPI interface{ Get(context.Context, string) (map[string]any, error) }
type UserAPI interface{ Get(context.Context, string) (map[string]any, error) }
type ShipmentAPI interface{ Track(context.Context, string) (map[string]any, error) }

func orderPage(ctx context.Context, orderID, userID string, o OrderAPI, u UserAPI, s ShipmentAPI) (map[string]any, error) {
	var wg sync.WaitGroup
	var order, user, shipment map[string]any
	var errO, errU error
	wg.Add(3)
	fetch := func(ms int, fn func(context.Context) error) {
		defer wg.Done()
		c, cancel := context.WithTimeout(ctx, time.Duration(ms)*time.Millisecond)
		defer cancel()
		_ = fn(c)
	}
	go fetch(300, func(c context.Context) error { order, errO = o.Get(c, orderID); return errO })
	go fetch(300, func(c context.Context) error { user, errU = u.Get(c, userID); return errU })
	go fetch(200, func(c context.Context) error {
		var err error
		shipment, err = s.Track(c, orderID)
		if errors.Is(err, context.DeadlineExceeded) {
			shipment = map[string]any{"status": "pending"}
			return nil
		}
		return err
	})
	wg.Wait()
	if errO != nil {
		return nil, errO
	}
	if errU != nil {
		return nil, errU
	}
	return map[string]any{"order": order, "user": user, "shipment": shipment}, nil
}
