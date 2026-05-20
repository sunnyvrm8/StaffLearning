// Scenario: order-confirmation notifications (email, SMS, push)
// Demonstrates: Strategy — ChannelSender per channel without editing dispatcher
// Trade-off: map registry vs switch; Go favors small interfaces + explicit errors

package main

import "fmt"

type ChannelSender interface {
	Channel() string
	Send(userID, body string) error
}

type emailSender struct{}

func (emailSender) Channel() string { return "email" }
func (emailSender) Send(_, _ string) error { return nil }

type NotificationDispatcher struct {
	senders map[string]ChannelSender
}

func NewNotificationDispatcher(list []ChannelSender) *NotificationDispatcher {
	m := make(map[string]ChannelSender, len(list))
	for _, s := range list {
		m[s.Channel()] = s
	}
	return &NotificationDispatcher{senders: m}
}

func (d *NotificationDispatcher) Notify(channel, userID, body string) error {
	s, ok := d.senders[channel]
	if !ok {
		return fmt.Errorf("unknown channel: %s", channel)
	}
	return s.Send(userID, body)
}
