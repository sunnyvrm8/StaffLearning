// Scenario: map domain failures to RFC 7807 Problem+JSON for public REST
// Demonstrates: stable type URI, title, status, retryable extension
// Trade-off: Go struct tags for JSON vs map — struct documents contract for codegen

package main

type ProblemDetail struct {
	Type      string `json:"type"`
	Title     string `json:"title"`
	Status    int    `json:"status"`
	Detail    string `json:"detail"`
	Instance  string `json:"instance,omitempty"`
	Retryable bool   `json:"retryable"`
}

const insufficientFundsType = "https://api.example.com/problems/insufficient-funds"

func problemFromDecline(orderID string, retryable bool) ProblemDetail {
	return ProblemDetail{
		Type:      insufficientFundsType,
		Title:     "Insufficient funds",
		Status:    402,
		Detail:    "Card declined for order " + orderID,
		Instance:  "/orders/" + orderID,
		Retryable: retryable,
	}
}
