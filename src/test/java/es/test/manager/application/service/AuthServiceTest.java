package es.test.manager.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.test.manager.application.dto.auth.AuthResponse;
import es.test.manager.application.dto.auth.LoginRequest;
import es.test.manager.application.dto.auth.RegisterRequest;
import es.test.manager.application.dto.auth.UserResponse;
import es.test.manager.application.mapper.UserDtoMapper;
import es.test.manager.domain.exception.InvalidCredentialsException;
import es.test.manager.domain.exception.InvalidEmailException;
import es.test.manager.domain.exception.InvalidUsernameException;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.Role;
import es.test.manager.domain.model.User;
import es.test.manager.domain.port.out.CredentialsAuthenticator;
import es.test.manager.domain.port.out.TokenService;
import es.test.manager.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String RAW_PASSWORD = "Password123";
    private static final String PASSWORD_HASH = "$2a$10$abcdefghijklmnopqrstuv";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CredentialsAuthenticator credentialsAuthenticator;

    @Mock
    private TokenService tokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                credentialsAuthenticator,
                tokenService,
                Mappers.getMapper(UserDtoMapper.class));
    }

    private static RegisterRequest registerRequest(String username) {
        return new RegisterRequest(username, "ana@example.com", RAW_PASSWORD, RAW_PASSWORD);
    }

    private static AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(5L, "ana", "ana@example.com", Role.USER);
    }

    @Test
    void registerStoresTheUsernameAsReceivedAndNeverStoresTheRawPassword() {
        when(userRepository.existsByUsername("  ana  ")).thenReturn(false);
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(7L);
            return user;
        });

        UserResponse response = authService.register(registerRequest("  ana  "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("  ana  ");
        assertThat(saved.getValue().getEmail()).isEqualTo("ana@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(response).isEqualTo(new UserResponse(7L, "  ana  ", "ana@example.com", Role.USER));
    }

    @Test
    void registerRejectsAnExistingUsernameWithoutSaving() {
        when(userRepository.existsByUsername("ana")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest("ana")))
                .isInstanceOf(InvalidUsernameException.class)
                .hasMessage("El username ya está en uso");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRejectsAnExistingEmailWithoutSaving() {
        when(userRepository.existsByUsername("ana")).thenReturn(false);
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest("ana")))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessage("El email ya está en uso");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginWithUsernameReturnsAToken() {
        when(credentialsAuthenticator.authenticate("ana", RAW_PASSWORD)).thenReturn(authenticatedUser());
        when(tokenService.generateToken(authenticatedUser())).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("ana", RAW_PASSWORD));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
    }

    @Test
    void loginWithEmailReturnsAToken() {
        when(credentialsAuthenticator.authenticate("ana@example.com", RAW_PASSWORD))
                .thenReturn(authenticatedUser());
        when(tokenService.generateToken(authenticatedUser())).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("ana@example.com", RAW_PASSWORD));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
    }

    @Test
    void loginDelegatesTheIdentifierWithoutTrimmingIt() {
        when(credentialsAuthenticator.authenticate("  ana", RAW_PASSWORD)).thenThrow(new InvalidCredentialsException());

        assertThatThrownBy(() -> authService.login(new LoginRequest("  ana", RAW_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(credentialsAuthenticator).authenticate("  ana", RAW_PASSWORD);
    }

    @Test
    void wrongPasswordAndUnknownUserThrowTheSameException() {
        when(credentialsAuthenticator.authenticate("ana", "Wrong123")).thenThrow(new InvalidCredentialsException());
        when(credentialsAuthenticator.authenticate("nobody", RAW_PASSWORD))
                .thenThrow(new InvalidCredentialsException());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana", "Wrong123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(new InvalidCredentialsException().getMessage());
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody", RAW_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(new InvalidCredentialsException().getMessage());

        verify(tokenService, never()).generateToken(any(AuthenticatedUser.class));
    }

    @Test
    void getCurrentUserMapsTheTokenClaims() {
        assertThat(authService.getCurrentUser(authenticatedUser()))
                .isEqualTo(new UserResponse(5L, "ana", "ana@example.com", Role.USER));
    }
}
