// Scenario: GET /orders?cursor=... for high-volume list API (no OFFSET on huge tables)
// Demonstrates: opaque cursor encoding keyset pagination
// Trade-off: stable under inserts vs OFFSET simplicity for admin-only small pages

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

record OrderRow(String id, String createdAt) {}
record Page<T>(List<T> items, String nextCursor) {}

final class CursorPagination {
    static String encodeCursor(String orderId, String createdAt) {
        String raw = orderId + "|" + createdAt;
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static String decodeOrderId(String cursor) {
        String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        return raw.split("\\|", 2)[0];
    }

    // Repository would: WHERE (created_at, id) < (?, ?) ORDER BY created_at DESC, id DESC LIMIT n+1
    static Page<OrderRow> page(List<OrderRow> fetched, int limit) {
        boolean hasMore = fetched.size() > limit;
        List<OrderRow> items = hasMore ? fetched.subList(0, limit) : fetched;
        String next = hasMore
            ? encodeCursor(items.get(items.size() - 1).id(), items.get(items.size() - 1).createdAt())
            : null;
        return new Page<>(items, next);
    }
}
