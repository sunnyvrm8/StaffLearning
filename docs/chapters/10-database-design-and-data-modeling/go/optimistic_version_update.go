// Scenario: inventory reservation updates a row with version check to avoid lost writes
// Demonstrates: optimistic concurrency via version column in UPDATE ... WHERE
// Trade-off: explicit retry in app vs long-lived row locks under pessimistic locking

package main

import (
	"context"
	"database/sql"
	"errors"
)

var ErrVersionConflict = errors.New("inventory version conflict")

func ReserveStock(ctx context.Context, db *sql.DB, sku string, qty int, expectedVersion int64) (int64, error) {
	res, err := db.ExecContext(ctx, `
		UPDATE inventory
		SET on_hand = on_hand - $1, version = version + 1
		WHERE sku = $2 AND version = $3 AND on_hand >= $1`,
		qty, sku, expectedVersion)
	if err != nil {
		return 0, err
	}
	n, _ := res.RowsAffected()
	if n == 1 {
		return expectedVersion + 1, nil
	}
	return 0, ErrVersionConflict
}
