// Scenario: consistent hashing ring for distributed cache
// Demonstrates: node lookup for a key and wrap-around behavior
// Trade-off: even distribution versus virtual node complexity

package case_studies.cache;

import java.util.NavigableMap;
import java.util.TreeMap;

public class ConsistentHashRing {
    private final NavigableMap<Integer, String> ring = new TreeMap<>();

    public void addNode(String nodeId, int hash) {
        ring.put(hash, nodeId);
    }

    public String locateNode(String key) {
        int hash = key.hashCode();
        if (ring.containsKey(hash)) {
            return ring.get(hash);
        }
        var tail = ring.tailMap(hash, false);
        if (!tail.isEmpty()) {
            return tail.firstEntry().getValue();
        }
        return ring.firstEntry().getValue();
    }
}
