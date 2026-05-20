// Scenario: checkout publishes OrderPlaced with partition key = orderId
// Demonstrates: ProducerMessage.Key routes all events for one order to same partition
// Trade-off: nil key round-robins partitions — breaks per-order ordering

package main

type ProduceRequest struct {
	Topic string
	Key   []byte
	Value []byte
}

func OrderPlaced(orderID string, json []byte) ProduceRequest {
	return ProduceRequest{
		Topic: "order-events",
		Key:   []byte(orderID),
		Value: json,
	}
}
