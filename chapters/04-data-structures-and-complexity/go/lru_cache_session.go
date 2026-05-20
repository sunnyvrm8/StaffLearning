// Scenario: cap in-memory checkout sessions per node (10k) with LRU eviction
// Demonstrates: O(1) get/put via map + container/list order
// Trade-off: explicit list vs sync.Map; shard or use ristretto/Caffeine at scale

package main

import "container/list"

type LruCacheSession[V any] struct {
	cap   int
	ll    *list.List
	items map[string]*list.Element
}

type entry[V any] struct {
	key string
	val V
}

func NewLruCacheSession[V any](capacity int) *LruCacheSession[V] {
	return &LruCacheSession[V]{
		cap:   capacity,
		ll:    list.New(),
		items: make(map[string]*list.Element, capacity),
	}
}

func (c *LruCacheSession[V]) Get(key string) (V, bool) {
	if el, ok := c.items[key]; ok {
		c.ll.MoveToFront(el)
		return el.Value.(*entry[V]).val, true
	}
	var zero V
	return zero, false
}

func (c *LruCacheSession[V]) Put(key string, val V) {
	if el, ok := c.items[key]; ok {
		c.ll.MoveToFront(el)
		el.Value.(*entry[V]).val = val
		return
	}
	el := c.ll.PushFront(&entry[V]{key: key, val: val})
	c.items[key] = el
	if c.ll.Len() > c.cap {
		back := c.ll.Back()
		c.ll.Remove(back)
		delete(c.items, back.Value.(*entry[V]).key)
	}
}
