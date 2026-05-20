// Scenario: GET /orders?cursor=... for high-volume list API (no OFFSET on huge tables)
// Demonstrates: opaque cursor encoding keyset pagination
// Trade-off: cursor invalidation on sort change — version cursor schema in API contract

package main

import (
	"encoding/base64"
	"strings"
)

type OrderRow struct {
	ID        string
	CreatedAt string
}

type Page[T any] struct {
	Items      []T
	NextCursor string
}

func encodeCursor(orderID, createdAt string) string {
	raw := orderID + "|" + createdAt
	return base64.RawURLEncoding.EncodeToString([]byte(raw))
}

func decodeOrderID(cursor string) string {
	b, _ := base64.RawURLEncoding.DecodeString(cursor)
	parts := strings.SplitN(string(b), "|", 2)
	return parts[0]
}

func pageOrders(fetched []OrderRow, limit int) Page[OrderRow] {
	hasMore := len(fetched) > limit
	items := fetched
	if hasMore {
		items = fetched[:limit]
	}
	var next string
	if hasMore {
		last := items[len(items)-1]
		next = encodeCursor(last.ID, last.CreatedAt)
	}
	return Page[OrderRow]{Items: items, NextCursor: next}
}
