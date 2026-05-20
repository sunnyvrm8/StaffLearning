// Scenario: cap in-memory checkout sessions per node (10k) with LRU eviction
// Demonstrates: O(1) get/put via LinkedHashMap access-order
// Trade-off: not thread-safe; production shards by sessionId or uses Caffeine

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class LruCacheSession<V> {
    private final int capacity;
    private final LinkedHashMap<String, V> map;

    LruCacheSession(int capacity) {
        this.capacity = capacity;
        this.map = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
                return size() > LruCacheSession.this.capacity;
            }
        };
    }

    Optional<V> get(String sessionId) {
        return Optional.ofNullable(map.get(sessionId));
    }

    void put(String sessionId, V state) {
        map.put(sessionId, state);
    }

    int size() { return map.size(); }
}
