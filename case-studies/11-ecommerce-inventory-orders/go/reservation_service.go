// Scenario: inventory reservation with outbox pattern in Go
// Demonstrates: reserve stock and publish a durable event for order processing
// Trade-off: reservation latency versus eventual downstream consistency

package ecommerce

import "errors"

var ErrOutOfStock = errors.New("out of stock")

func ReserveOrder(order Order, inventory InventoryService, outbox Outbox) error {
	ok, err := inventory.Reserve(order.Items)
	if err != nil {
		return err
	}
	if !ok {
		return ErrOutOfStock
	}

	return outbox.Publish(OrderReserved{OrderID: order.ID})
}
