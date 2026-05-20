// Scenario: POST /charges with client-supplied idempotency key (payments API)
// Demonstrates: lookup → lock → execute → cache response for safe retries
// Trade-off: Redis TTL store vs DB uniqueness — Redis faster, DB stronger durability

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

record ChargeRequest(String idempotencyKey, long amountCents) {}
record ChargeResponse(String chargeId, int httpStatus) {}

interface IdempotencyStore {
    Optional<ChargeResponse> get(String key);
    boolean tryLock(String key);
    void save(String key, ChargeResponse response);
}

final class InMemoryIdempotencyStore implements IdempotencyStore {
    private final ConcurrentHashMap<String, ChargeResponse> done = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> locks = new ConcurrentHashMap<>();

    public Optional<ChargeResponse> get(String key) { return Optional.ofNullable(done.get(key)); }
    public boolean tryLock(String key) { return locks.putIfAbsent(key, true) == null; }
    public void save(String key, ChargeResponse response) {
        done.put(key, response);
        locks.remove(key);
    }
}

final class ChargeHandler {
    private final IdempotencyStore store;

    ChargeHandler(IdempotencyStore store) { this.store = store; }

    ChargeResponse charge(ChargeRequest req) {
        return store.get(req.idempotencyKey())
            .orElseGet(() -> {
                if (!store.tryLock(req.idempotencyKey())) {
                    return new ChargeResponse("", 409);
                }
                ChargeResponse created = new ChargeResponse("ch_" + req.amountCents(), 201);
                store.save(req.idempotencyKey(), created);
                return created;
            });
    }
}
