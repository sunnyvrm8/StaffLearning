// Scenario: consistent hashing ring lookup for distributed cache
// Demonstrates: key-to-node mapping and ring wrap-around
// Trade-off: simple hashing versus virtual node balancing

package cache

type ConsistentHashRing struct {
	points []int
	nodes  map[int]string
}

func (r *ConsistentHashRing) LocateNode(key string) string {
	hash := hashFn(key)
	for _, point := range r.points {
		if point >= hash {
			return r.nodes[point]
		}
	}
	return r.nodes[r.points[0]]
}

func hashFn(key string) int {
	h := 0
	for _, c := range key {
		h = 31*h + int(c)
	}
	return h & 0x7fffffff
}
