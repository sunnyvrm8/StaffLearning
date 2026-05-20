// Scenario: payment webhook may retry; insert ledger row once per provider event id
// Demonstrates: ON CONFLICT DO NOTHING for idempotent ledger append
// Trade-off: Postgres-specific upsert vs portable unique violation handling

package main

import (
	"context"
	"database/sql"
	"errors"
)

var ErrDuplicateEvent = errors.New("duplicate payment event")

func RecordWebhook(ctx context.Context, db *sql.DB, provider, eventID, payload string) error {
	_, err := db.ExecContext(ctx, `
		INSERT INTO payment_events (provider, event_id, payload)
		VALUES ($1, $2, $3)
		ON CONFLICT (provider, event_id) DO NOTHING`,
		provider, eventID, payload)
	if err != nil {
		return err
	}
	// optional: SELECT to distinguish inserted vs duplicate for metrics
	return nil
}
