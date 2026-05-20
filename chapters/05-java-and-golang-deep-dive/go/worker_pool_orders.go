// Scenario: fan-out price checks for N line items with bounded concurrency
// Demonstrates: worker pool + channels — backpressure without unbounded goroutines
// Trade-off: more structure than errgroup+semaphore; explicit queue ownership

package main

import "context"

type Pricing interface {
	Price(ctx context.Context, sku string) (float64, error)
}

type priceJob struct {
	idx int
	sku string
}

type priceResult struct {
	idx int
	val float64
	err error
}

func Prices(ctx context.Context, skus []string, pricing Pricing, workers int) ([]float64, error) {
	jobs := make(chan priceJob)
	results := make(chan priceResult)

	for w := 0; w < workers; w++ {
		go func() {
			for j := range jobs {
				p, err := pricing.Price(ctx, j.sku)
				results <- priceResult{idx: j.idx, val: p, err: err}
			}
		}()
	}

	go func() {
		for i, sku := range skus {
			jobs <- priceJob{idx: i, sku: sku}
		}
		close(jobs)
	}()

	out := make([]float64, len(skus))
	for range skus {
		r := <-results
		if r.err != nil {
			return nil, r.err
		}
		out[r.idx] = r.val
	}
	return out, nil
}
