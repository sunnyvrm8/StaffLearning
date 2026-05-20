// Scenario: "trending now" — top K product IDs by event count in a 5-minute window
// Demonstrates: container/heap min-heap of size K
// Trade-off: streaming use case may prefer Count-Min Sketch instead of exact map

package main

import "container/heap"

type ProductCount struct {
	ID    string
	Count int64
}

type minHeap []ProductCount

func (h minHeap) Len() int            { return len(h) }
func (h minHeap) Less(i, j int) bool  { return h[i].Count < h[j].Count }
func (h minHeap) Swap(i, j int)       { h[i], h[j] = h[j], h[i] }
func (h *minHeap) Push(x any)         { *h = append(*h, x.(ProductCount)) }
func (h *minHeap) Pop() any {
	old := *h
	n := len(old)
	x := old[n-1]
	*h = old[:n-1]
	return x
}

func TopK(counts map[string]int64, k int) []ProductCount {
	h := &minHeap{}
	heap.Init(h)
	for id, c := range counts {
		heap.Push(h, ProductCount{ID: id, Count: c})
		if h.Len() > k {
			heap.Pop(h)
		}
	}
	out := make([]ProductCount, h.Len())
	for i := h.Len() - 1; i >= 0; i-- {
		out[i] = heap.Pop(h).(ProductCount)
	}
	return out
}
