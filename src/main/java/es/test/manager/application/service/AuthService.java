package es.test.manager.application.service;

import es.test.manager.application.dto.auth.AuthResponse;
import es.test.manager.application.dto.auth.LoginRequest;
import es.test.manager.application.dto.auth.RegisterRequest;
import es.test.manager.application.dto.auth.UserResponse;
import es.test.manager.domain.exception.InvalidEmailException;
import es.test.manager.domain.exception.InvalidUsernameException;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.Role;
import es.test.manager.domain.model.User;
import es.test.manager.domain.port.in.LoginUseCase;
import es.test.manager.domain.port.in.RegisterUseCase;
import es.test.manager.domain.port.in.SessionUseCase;
import es.test.manager.domain.port.out.CredentialsAuthenticator;
import es.test.manager.domain.port.out.TokenService;
import es.test.manager.domain.port.out.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service implementing the authentication use cases. */
@Service
@RequiredArgsConstructor
public class AuthService implements RegisterUseCase, LoginUseCase, SessionUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CredentialsAuthenticator credentialsAuthenticator;
    private final TokenService tokenService;

    /** RF-001, RF-002, RF-006, RF-007, RF-009. */
    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new InvalidUsernameException();
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new InvalidEmailException();
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .role(Role.USER)
                .passwordHash(passwordEncoder.encode(request.password()))
                .createdAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRole());
    }

    /** RF-010, RF-011, RF-013: credentials are checked by Spring Security's authentication chain. */
    @Override
    public AuthResponse login(LoginRequest request) {
        AuthenticatedUser user = credentialsAuthenticator.authenticate(request.identifier(), request.password());
        return new AuthResponse(tokenService.generateToken(user));
    }

    /** RF-014: the identity comes from the token claims, no database round trip. */
    @Override
    public UserResponse getCurrentUser(AuthenticatedUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
