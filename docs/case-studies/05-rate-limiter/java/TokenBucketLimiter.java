// Scenario: distributed rate limiting using token bucket semantics
// Demonstrates: local refill and atomic decrement for request allowance
// Trade-off: approximate burst handling versus strict global limits

package case_studies.ratelimiter;

public class TokenBucketLimiter {
    private final RateStore rateStore;
    private final int capacity;
    private final double refillRate;

    public TokenBucketLimiter(RateStore rateStore, int capacity, double refillRate) {
        this.rateStore = rateStore;
        this.capacity = capacity;
        this.refillRate = refillRate;
    }

    public boolean allow(String key) {
        TokenBucket bucket = rateStore.get(key);
        long now = System.currentTimeMillis();
        double tokens = Math.min(capacity, bucket.tokens + (now - bucket.lastUpdated) * refillRate / 1000.0);
        if (tokens < 1) {
            return false;
        }
        bucket.tokens = tokens - 1;
        bucket.lastUpdated = now;
        rateStore.save(key, bucket);
        return true;
    }
}
