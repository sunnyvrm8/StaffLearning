// Scenario: refresh token validation in Go
// Demonstrates: session lookup, revocation check, and access token issuance
// Trade-off: session store use versus completely stateless refresh

package auth

import (
	"errors"
	"time"
)

type SessionStore interface {
	GetByRefreshToken(token string) (*Session, error)
}

var ErrUnauthorized = errors.New("unauthorized")

type TokenService interface {
	IssueAccessToken(userID string) (string, error)
}

type RefreshRequest struct {
	RefreshToken string
}

type AuthResponse struct {
	AccessToken string
}

type Session struct {
	UserID    string
	Revoked   bool
	ExpiresAt int64
}

func RefreshToken(req RefreshRequest, store SessionStore, tokenService TokenService) (*AuthResponse, error) {
	session, err := store.GetByRefreshToken(req.RefreshToken)
	if err != nil {
		return nil, err
	}
	if session == nil || session.Revoked || time.Now().Unix() > session.ExpiresAt {
		return nil, ErrUnauthorized
	}

	accessToken, err := tokenService.IssueAccessToken(session.UserID)
	if err != nil {
		return nil, err
	}

	return &AuthResponse{AccessToken: accessToken}, nil
}
