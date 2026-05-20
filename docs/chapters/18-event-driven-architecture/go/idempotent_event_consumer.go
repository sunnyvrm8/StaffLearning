// Scenario: fulfillment consumes OrderPlaced; broker may redeliver
// Demonstrates: check processed_events before creating shipment
// Trade-off: ledger table growth — partition/archive by TTL policy

package main

import (
	"context"
	"database/sql"
	"errors"
)

func onOrderPlaced(ctx context.Context, db *sql.DB, eventID, orderID string) error {
	tx, err := db.BeginTx(ctx, nil)
	if err != nil {
		return err
	}
	defer tx.Rollback()

	var exists int
	err = tx.QueryRowContext(ctx,
		`SELECT 1 FROM processed_events WHERE event_id = $1`, eventID).Scan(&exists)
	if err == nil {
		return nil // duplicate delivery — safe no-op
	}
	if !errors.Is(err, sql.ErrNoRows) {
		return err
	}
	if _, err = tx.ExecContext(ctx,
		`INSERT INTO processed_events (event_id) VALUES ($1)`, eventID); err != nil {
		return err
	}
	if _, err = tx.ExecContext(ctx,
		`INSERT INTO shipments (order_id, status) VALUES ($1, 'PENDING')`, orderID); err != nil {
		return err
	}
	return tx.Commit()
}
