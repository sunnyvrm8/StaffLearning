// Scenario: order placement charges via PSP without importing Stripe SDK in domain
// Demonstrates: Dependency Inversion — OrderService depends on PaymentPort
// Trade-off: extra interface vs testability and PSP swap

import java.util.Optional;

record ChargeRequest(String orderId, long amountCents, String currency) {}
record ChargeResult(String providerRef, boolean success) {}

interface PaymentPort {
    ChargeResult charge(ChargeRequest request);
}

final class OrderService {
    private final PaymentPort payments;
    OrderService(PaymentPort payments) { this.payments = payments; }

    Optional<String> placeOrder(ChargeRequest req) {
        ChargeResult res = payments.charge(req);
        return res.success() ? Optional.of(res.providerRef()) : Optional.empty();
    }
}

// Infrastructure adapter (would live in infra package in production)
final class StripeAdapter implements PaymentPort {
    public ChargeResult charge(ChargeRequest request) {
        // call Stripe SDK here
        return new ChargeResult("ch_mock", true);
    }
}
