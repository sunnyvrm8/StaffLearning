// Scenario: order-confirmation notifications (email, SMS, push)
// Demonstrates: Strategy — ChannelSender per channel without editing dispatcher
// Trade-off: registry vs switch; ensure selection is observable (metrics/logs)

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

interface ChannelSender {
    String channel();
    void send(String userId, String body);
}

final class EmailSender implements ChannelSender {
    public String channel() { return "email"; }
    public void send(String userId, String body) { /* SES/SMTP */ }
}

final class NotificationDispatcher {
    private final Map<String, ChannelSender> senders;
    NotificationDispatcher(java.util.List<ChannelSender> list) {
        this.senders = list.stream().collect(Collectors.toMap(ChannelSender::channel, Function.identity()));
    }
    void notify(String channel, String userId, String body) {
        ChannelSender s = senders.get(channel);
        if (s == null) throw new IllegalArgumentException("unknown channel: " + channel);
        s.send(userId, body);
    }
}
