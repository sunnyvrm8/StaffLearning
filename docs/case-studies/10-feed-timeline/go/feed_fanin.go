// Scenario: feed fan-in on read for a timeline service
// Demonstrates: assembling a feed from followed sources at read time
// Trade-off: cheaper writes versus more expensive reads

package feed

type TimelineStore interface {
	RecentFor(userID string) ([]TimelineEntry, error)
}

type Reranker interface {
	Score([]TimelineEntry) []TimelineEntry
}

type Service struct {
	store    TimelineStore
	reranker Reranker
}

func (s *Service) GetFeed(userID string) ([]TimelineEntry, error) {
	posts, err := s.store.RecentFor(userID)
	if err != nil {
		return nil, err
	}
	return s.reranker.Score(posts), nil
}
