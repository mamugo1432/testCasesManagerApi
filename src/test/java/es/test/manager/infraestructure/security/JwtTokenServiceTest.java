package es.test.manager.infraestructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.test.manager.domain.exception.InvalidTokenException;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

/** Token issuance, claims and rejection of expired or tampered tokens (RF-010, RF-012, RF-014, RF-015). */
class JwtTokenServiceTest {

    private static final String SECRET = "test-secret-0123456789-0123456789-0123456789";

    private static AuthenticatedUser user() {
        return new AuthenticatedUser(5L, "ana", "ana@example.com", Role.ADMIN);
    }

    @Test
    void generatedTokenCarriesTheUserClaims() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, Duration.ofHours(24));

        AuthenticatedUser extracted = tokenService.extractUser(tokenService.generateToken(user()));

        assertThat(extracted.getId()).isEqualTo(5L);
        assertThat(extracted.getUsername()).isEqualTo("ana");
        assertThat(extracted.getEmail()).isEqualTo("ana@example.com");
        assertThat(extracted.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void tokenWithoutRoleClaimDefaultsToUser() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject("5")
                .claim("username", "ana")
                .claim("email", "ana@example.com")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)))
                .signWith(key)
                .compact();
        JwtTokenService tokenService = new JwtTokenService(SECRET, Duration.ofHours(24));

        assertThat(tokenService.extractUser(token).getRole()).isEqualTo(Role.USER);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, Duration.ofSeconds(-1));
        String expiredToken = tokenService.generateToken(user());

        assertThatThrownBy(() -> tokenService.extractUser(expiredToken)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, Duration.ofHours(1));
        String token = tokenService.generateToken(user());
        String[] parts = token.split("\\.");
        char[] payload = parts[1].toCharArray();
        payload[0] = payload[0] == 'a' ? 'b' : 'a';
        String tampered = parts[0] + "." + new String(payload) + "." + parts[2];

        assertThatThrownBy(() -> tokenService.extractUser(tampered)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void malformedOrMissingTokenIsRejected() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, Duration.ofHours(1));

        assertThatThrownBy(() -> tokenService.extractUser("not-a-token")).isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> tokenService.extractUser(null)).isInstanceOf(InvalidTokenException.class);
    }
}
