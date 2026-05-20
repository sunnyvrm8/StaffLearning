// Scenario: checkout order — lines cannot change after submit
// Demonstrates: Aggregate root enforcing invariants
// Trade-off: exported fields vs methods; keep invariants inside one type

package main

import "errors"

type OrderStatus string

const (
	StatusDraft     OrderStatus = "DRAFT"
	StatusSubmitted OrderStatus = "SUBMITTED"
)

type LineItem struct {
	SKU string
	Qty int
}

type Order struct {
	ID     string
	status OrderStatus
	lines  []LineItem
}

func NewOrder(id string) *Order {
	return &Order{ID: id, status: StatusDraft}
}

func (o *Order) AddLine(item LineItem) error {
	if o.status != StatusDraft {
		return errors.New("cannot modify submitted order")
	}
	o.lines = append(o.lines, item)
	return nil
}

func (o *Order) Submit() error {
	if len(o.lines) == 0 {
		return errors.New("empty order")
	}
	o.status = StatusSubmitted
	return nil
}
