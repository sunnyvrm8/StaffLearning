// Scenario: fulfillment consumes OrderPlaced from Kafka (at-least-once delivery)
// Demonstrates: dedup by eventId before side effect; remove key on transient failure
// Trade-off: in-memory set is handbook-only — production uses UNIQUE(event_id) in DB

package main

import "sync"

type IdempotentOrderConsumer struct {
	mu        sync.Mutex
	processed map[string]struct{}
}

func (c *IdempotentOrderConsumer) OnRecord(eventID, orderID string, reserve func() error) error {
	if c.seen(eventID) {
		return nil
	}
	if err := reserve(); err != nil {
		c.forget(eventID)
		return err
	}
	return nil
}

func (c *IdempotentOrderConsumer) seen(id string) bool {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.processed == nil {
		c.processed = make(map[string]struct{})
	}
	if _, ok := c.processed[id]; ok {
		return true
	}
	c.processed[id] = struct{}{}
	return false
}

func (c *IdempotentOrderConsumer) forget(id string) {
	c.mu.Lock()
	defer c.mu.Unlock()
	delete(c.processed, id)
}
