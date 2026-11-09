package es.test.manager.infraestructure.security;

import es.test.manager.domain.exception.InvalidTokenException;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.Role;
import es.test.manager.domain.port.out.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** JWT adapter (jjwt 0.12.6) implementing the {@link TokenService} port. */
@Component
public class JwtTokenService implements TokenService {

    private static final String USERNAME_CLAIM = "username";
    private static final String EMAIL_CLAIM = "email";
    private static final String ROLE_CLAIM = "role";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration expiration;

    public JwtTokenService(
            @Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration}") Duration expiration) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("app.jwt.secret debe tener al menos 32 bytes (256 bits)");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expiration = expiration;
    }

    @Override
    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(USERNAME_CLAIM, user.getUsername())
                .claim(EMAIL_CLAIM, user.getEmail())
                .claim(
                        ROLE_CLAIM,
                        user.getRole() == null
                                ? Role.USER.name()
                                : user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    @Override
    public AuthenticatedUser extractUser(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String role = claims.get(ROLE_CLAIM, String.class);
            return AuthenticatedUser.builder()
                    .id(Long.valueOf(claims.getSubject()))
                    .username(claims.get(USERNAME_CLAIM, String.class))
                    .email(claims.get(EMAIL_CLAIM, String.class))
                    .role(role == null ? Role.USER : Role.valueOf(role))
                    .build();
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidTokenException();
        }
    }
}
