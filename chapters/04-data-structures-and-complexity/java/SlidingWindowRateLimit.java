// Scenario: per-user API rate limit — max 100 requests per 60-second sliding window
// Demonstrates: deque of timestamps; prune stale on each check
// Trade-off: memory per active user vs fixed-window counter (cheaper, burstier)

import java.util.ArrayDeque;
import java.util.Deque;

final class SlidingWindowRateLimit {
    private final int maxRequests;
    private final long windowMillis;
    private final Deque<Long> timestamps = new ArrayDeque<>();

    SlidingWindowRateLimit(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    synchronized boolean allow(long nowMillis) {
        while (!timestamps.isEmpty() && nowMillis - timestamps.peekFirst() >= windowMillis) {
            timestamps.pollFirst();
        }
        if (timestamps.size() >= maxRequests) return false;
        timestamps.addLast(nowMillis);
        return true;
    }
}
