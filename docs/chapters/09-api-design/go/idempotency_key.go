// Scenario: POST /charges with client-supplied idempotency key (payments API)
// Demonstrates: lookup → lock → execute → cache response for safe retries
// Trade-off: explicit error return vs Java Optional — same semantics, Go store interface

package main

import "sync"

type ChargeRequest struct {
	IdempotencyKey string
	AmountCents    int64
}

type ChargeResponse struct {
	ChargeID   string
	HTTPStatus int
}

type IdempotencyStore interface {
	Get(key string) (ChargeResponse, bool)
	TryLock(key string) bool
	Save(key string, resp ChargeResponse)
}

type memoryStore struct {
	mu    sync.Mutex
	done  map[string]ChargeResponse
	locks map[string]struct{}
}

func (s *memoryStore) Get(key string) (ChargeResponse, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	r, ok := s.done[key]
	return r, ok
}

func (s *memoryStore) TryLock(key string) bool {
	s.mu.Lock()
	defer s.mu.Unlock()
	if _, held := s.locks[key]; held {
		return false
	}
	s.locks[key] = struct{}{}
	return true
}

func (s *memoryStore) Save(key string, resp ChargeResponse) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.done[key] = resp
	delete(s.locks, key)
}

func charge(store IdempotencyStore, req ChargeRequest) ChargeResponse {
	if r, ok := store.Get(req.IdempotencyKey); ok {
		return r
	}
	if !store.TryLock(req.IdempotencyKey) {
		return ChargeResponse{HTTPStatus: 409}
	}
	created := ChargeResponse{ChargeID: "ch_ok", HTTPStatus: 201}
	store.Save(req.IdempotencyKey, created)
	return created
}
