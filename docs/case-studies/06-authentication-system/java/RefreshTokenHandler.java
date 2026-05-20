// Scenario: refresh token validation in an authentication system
// Demonstrates: session lookup, revocation check, and access token issuance
// Trade-off: session store dependency versus stateless token reuse

package case_studies.auth;

public class RefreshTokenHandler {
    private final SessionStore sessionStore;
    private final TokenService tokenService;

    public RefreshTokenHandler(SessionStore sessionStore, TokenService tokenService) {
        this.sessionStore = sessionStore;
        this.tokenService = tokenService;
    }

    public AuthResponse refresh(RefreshRequest request) {
        Session session = sessionStore.getByRefreshToken(request.getRefreshToken());
        if (session == null || session.isRevoked() || session.isExpired()) {
            throw new UnauthorizedException();
        }

        String accessToken = tokenService.issueAccessToken(session.getUserId());
        return new AuthResponse(accessToken);
    }
}
