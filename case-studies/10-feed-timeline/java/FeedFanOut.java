// Scenario: feed fan-out on write for a timeline service
// Demonstrates: writing new content to followers' timelines
// Trade-off: write amplification versus fast read latency

package case_studies.feed;

import java.util.List;

public class FeedFanOut {
    private final TimelineStore timelineStore;

    public FeedFanOut(TimelineStore timelineStore) {
        this.timelineStore = timelineStore;
    }

    public void fanOut(String authorId, List<String> followerIds, TimelineEntry entry) {
        for (String followerId : followerIds) {
            timelineStore.append(followerId, entry);
        }
    }
}
