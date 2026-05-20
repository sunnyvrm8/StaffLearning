// Scenario: idempotent payment creation in a payment system
// Demonstrates: idempotency lookup, provider authorization, and response caching
// Trade-off: storage overhead versus duplicate-charge safety

package payment

type PaymentCoordinator struct {
	IdempotencyStore IdempotencyStore
	PaymentStore     PaymentStore
	Provider         PaymentProvider
}

func (c *PaymentCoordinator) HandleCreate(req PaymentRequest) (PaymentResponse, error) {
	if rec, err := c.IdempotencyStore.Lookup(req.IdempotencyKey); err == nil && rec != nil {
		return rec.Response, nil
	}

	payment, err := c.PaymentStore.Create(req)
	if err != nil {
		return PaymentResponse{}, err
	}

	response, err := c.Provider.Authorize(payment)
	if err != nil {
		return PaymentResponse{}, err
	}

	if err := c.IdempotencyStore.Save(req.IdempotencyKey, response); err != nil {
		return PaymentResponse{}, err
	}

	return response, nil
}
