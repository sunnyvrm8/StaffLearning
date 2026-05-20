// Scenario: idempotent payment creation in a payment system
// Demonstrates: idempotency lookup, provider interaction, and response caching
// Trade-off: simplicity versus storage overhead for idempotency records

package case_studies.payment;

public class PaymentCoordinator {
    private final IdempotencyStore idempotencyStore;
    private final PaymentStore paymentStore;
    private final PaymentProvider provider;

    public PaymentCoordinator(IdempotencyStore idempotencyStore, PaymentStore paymentStore, PaymentProvider provider) {
        this.idempotencyStore = idempotencyStore;
        this.paymentStore = paymentStore;
        this.provider = provider;
    }

    public PaymentResponse handleCreate(PaymentRequest request) {
        IdempotencyRecord existing = idempotencyStore.lookup(request.getIdempotencyKey());
        if (existing != null) {
            return existing.getResponse();
        }

        PaymentRecord record = paymentStore.create(request);
        PaymentResponse response = provider.authorize(record);
        idempotencyStore.save(request.getIdempotencyKey(), response);
        return response;
    }
}
