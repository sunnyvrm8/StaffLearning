// Scenario: reserve stock via modern WMS or legacy warehouse API
// Demonstrates: Port in domain + Adapter in infrastructure
// Trade-off: explicit error mapping at boundary vs bool-only port

package main

type InventoryPort interface {
	Reserve(sku string, qty int) error
}

type OrderService struct{ inventory InventoryPort }

func (s *OrderService) Place(sku string, qty int) error {
	return s.inventory.Reserve(sku, qty)
}

type LegacyWarehouseAdapter struct{}

func (LegacyWarehouseAdapter) Reserve(string, int) error {
	// call legacy API; map vendor errors to domain errors here
	return nil
}
