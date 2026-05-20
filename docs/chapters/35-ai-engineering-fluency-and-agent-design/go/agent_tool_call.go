package agent

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"net/http"
	"strings"
)

// Scenario: orchestrator executes model-proposed refund with shared idempotency semantics as Java sample.
// Demonstrates: context-bounded POST; SHA-256 key from tenant|tool|body for dedupe on retry.
// Trade-off: http.DefaultClient has no global dial timeout—production uses a tuned *http.Client transport.
func Refund(ctx context.Context, tenantID, orderID, amount string) (string, error) {
	body := fmt.Sprintf(`{"orderId":%q,"amount":%q}`, orderID, amount)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, "https://payments.example/refunds", strings.NewReader(body))
	if err != nil {
		return "", err
	}
	req.Header.Set("Idempotency-Key", idempotencyKey(tenantID, "refund", body))
	req.Header.Set("Content-Type", "application/json")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	out, err := io.ReadAll(resp.Body)
	return string(out), err
}

func idempotencyKey(tenant, tool, canonical string) string {
	sum := sha256.Sum256([]byte(tenant + "|" + tool + "|" + canonical))
	return hex.EncodeToString(sum[:])[:32]
}
