// Scenario: order placement charges via PSP without importing Stripe SDK in domain
// Demonstrates: Dependency Inversion — OrderService depends on PaymentPort interface
// Trade-off: explicit error returns vs exceptions; same boundary as Java

package main

type ChargeRequest struct {
	OrderID      string
	AmountCents  int64
	Currency     string
}

type ChargeResult struct {
	ProviderRef string
	Success     bool
}

type PaymentPort interface {
	Charge(req ChargeRequest) (ChargeResult, error)
}

type OrderService struct{ payments PaymentPort }

func (s *OrderService) PlaceOrder(req ChargeRequest) (string, bool) {
	res, err := s.payments.Charge(req)
	if err != nil || !res.Success {
		return "", false
	}
	return res.ProviderRef, true
}

type stripeAdapter struct{}

func (stripeAdapter) Charge(req ChargeRequest) (ChargeResult, error) {
	return ChargeResult{ProviderRef: "ch_mock", Success: true}, nil
}
