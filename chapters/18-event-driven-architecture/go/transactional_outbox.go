// Scenario: checkout commits Order + outbox row; worker publishes asynchronously
// Demonstrates: transactional outbox in one DB transaction
// Trade-off: publish lag acceptable for async fulfillment; not for sync read-your-writes

package main

import (
	"context"
	"database/sql"
	"encoding/json"
)

type OutboxRow struct {
	EventID string
	Topic   string
	Payload []byte
}

func placeOrder(ctx context.Context, tx *sql.Tx, orderID, userID string) error {
	if _, err := tx.ExecContext(ctx,
		`INSERT INTO orders (id, user_id, status) VALUES ($1, $2, 'PLACED')`,
		orderID, userID); err != nil {
		return err
	}
	payload, _ := json.Marshal(map[string]string{
		"type": "OrderPlaced", "orderId": orderID,
	})
	_, err := tx.ExecContext(ctx,
		`INSERT INTO outbox (event_id, topic, payload, published) VALUES (gen_random_uuid(), $1, $2, false)`,
		"order.events", payload)
	return err
}
