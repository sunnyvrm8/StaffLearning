// Scenario: "trending now" — top K product IDs by event count in a 5-minute window
// Demonstrates: min-heap of size K beats full sort when K << n
// Trade-off: exact counts need map; heap holds only K candidates

import java.util.*;

record ProductCount(String productId, long count) {}

final class TopKHotProducts {
    List<ProductCount> topK(Map<String, Long> counts, int k) {
        PriorityQueue<ProductCount> minHeap = new PriorityQueue<>(Comparator.comparingLong(ProductCount::count));
        for (var e : counts.entrySet()) {
            minHeap.offer(new ProductCount(e.getKey(), e.getValue()));
            if (minHeap.size() > k) minHeap.poll();
        }
        ArrayList<ProductCount> out = new ArrayList<>(minHeap);
        out.sort(Comparator.comparingLong(ProductCount::count).reversed());
        return out;
    }
}
